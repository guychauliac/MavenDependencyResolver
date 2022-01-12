package org.chabernac.dependency;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.chabernac.maven.repository.IRepository;

public class ResolveDependenciesWorker implements Supplier<Set<Dependency>> {
    private static final Logger                   LOGGER             = LogManager.getLogger( ResolveDependenciesWorker.class );
    private final Model                           project;
    private final IRepository                     repository;
    private final IPOMUtils                       pomUtils           = new POMUtils();
    private final Function<GAV, Optional<String>> resolveRange;

    private Map<GAV, Set<Dependency>>             cachedDependencies = new HashMap<>();

    public ResolveDependenciesWorker( Model project,
                                      IRepository repository ) {
        super();
        if ( project == null ) {
            throw new IllegalArgumentException( "input project must not be null" );
        }
        this.project = project;
        this.repository = repository;
        this.resolveRange = new ResolveRange( repository );
    }

    @Override
    public Set<Dependency> get() {
        return processPomStream( project, null, true );
    }

    private Set<Dependency> processPomStream( Model model, List<Exclusion> exclusions, boolean isRootPOM ) {
        GAV gav = GAV.fromModel( model );
        if ( cachedDependencies.containsKey( gav ) ) {
            LOGGER.debug( "Returning cached dependencies for: {}", gav );
            return cachedDependencies.get( gav );
        }

        try {
            Set<Dependency> dependencies = new LinkedHashSet<>();
            cachedDependencies.put( gav, dependencies );
            LOGGER.debug( "Retrieving dependencies for: {}", gav );
            boolean allParentsLoaded = getConfigurationsFromParent( model );
            resolveImportedDependencies( model );
            applyDependencyManagement( model, allParentsLoaded );
            resolveRanges( model );
            addDependenciesWithValidScopeToList( dependencies, model, exclusions, isRootPOM );
            addTransitiveDependencieswWithValidScopeToList( dependencies, model, exclusions, isRootPOM );
            if ( becauseDependencyManagementIsNotTransitiveOnlyApplyOnRootPom( isRootPOM ) ) {
                copyVersionsFromDependencyManagement( dependencies, model.getDependencyManagement().getDependencies() );
            }
            return removeDuplicates( dependencies );
        } catch ( DepencyResolvingException e ) {
            throw e;
        } catch ( Exception e ) {
            throw new DepencyResolvingException( "Could not resolve dependencies", e );
        }
    }

    private Set<Dependency> removeDuplicates( Set<Dependency> dependencies ) {
        return dependencies.stream()
            .map( dependency -> new ComparableDependency( dependency ) )
            .distinct()
            .map( dependency -> dependency.getDependency() )
            .collect( Collectors.toSet() );
    }

    private void resolveRanges( Model model ) {
        model.getDependencies()
            .stream()
            .filter( dependency -> ResolveRange.isRange( dependency.getVersion() ) )
            .forEach( dependency -> resolveVersionRange( dependency ) );

    }

    private void resolveVersionRange( Dependency dependency ) {
        resolveRange.apply( GAV.fromDependency( dependency ) ).ifPresent( version -> dependency.setVersion( version ) );
    }

    private void removeDoubleDependencies( Model model ) {
        List<Dependency> dependenciesWhichCanBeRemoved = model.getDependencies()
            .stream()
            .filter( dependency -> StringUtils.isEmpty( dependency.getVersion() ) )
            .filter( dependency -> modelHasVersionInOtherDependency( dependency, model ) )
            .collect( Collectors.toList() );

        model.getDependencies().removeAll( dependenciesWhichCanBeRemoved );

    }

    private boolean modelHasVersionInOtherDependency( Dependency dependencyWithoutVersion, Model model ) {
        return model.getDependencies()
            .stream()
            .filter( dependency -> dependency.getGroupId().equals( dependencyWithoutVersion.getGroupId() ) )
            .filter( dependency -> dependency.getArtifactId().equals( dependencyWithoutVersion.getArtifactId() ) )
            .anyMatch( dependency -> !StringUtils.isEmpty( dependency.getVersion() ) );
    }

    private boolean becauseDependencyManagementIsNotTransitiveOnlyApplyOnRootPom( boolean isRootPOM ) {
        return isRootPOM;
    }

    private void resolveProperties( Dependency dependency, Model model ) {
        int times = 0;
        while ( hasPropertyValue( dependency ) ) {
            resolveGAV( dependency, model );
            if ( times++ == 10 ) {
                return;
            }
        }
    }

    private boolean hasPropertyValue( Dependency dependency ) {
        return pomUtils.isPropertyValue( dependency.getGroupId() ) ||
               pomUtils.isPropertyValue( dependency.getArtifactId() ) ||
               pomUtils.isPropertyValue( dependency.getVersion() );
    }

    private void addDependenciesWithValidScopeToList( Set<Dependency> dependencies, Model model, List<Exclusion> exclusions, boolean inclusiveTestScope ) {
        model.getDependencies()
            .stream()
            .filter( dependency -> !dependency.isOptional() )
            .filter( dependency -> isValidScope( dependency.getScope(), inclusiveTestScope ) )
            .filter( dependency -> !isExcluded( dependency, exclusions ) )
            .forEach( dependency -> dependencies.add( dependency ) );
    }

    private void addTransitiveDependencieswWithValidScopeToList( Set<Dependency> dependencies, Model model, List<Exclusion> exclusions, boolean inclusiveTestScope ) {
        model.getDependencies()
            .stream()
            .filter( dependency -> !dependency.isOptional() )
            .filter( dependency -> isValidScope( dependency.getScope(), inclusiveTestScope ) )
            .filter( dependency -> !isExcluded( dependency, exclusions ) )
            .forEach( dependency -> dependencies.addAll( filterDependenciesAlreadyAdded( getTransitiveDependencies( dependency ), dependencies ) ) );
    }

    private void resolveImportedDependencies( Model model ) {
        Set<Dependency> importedManagedDependencies = getManagedDependencyImports( model );
        copyNonExistingDependencies( importedManagedDependencies, model.getDependencyManagement().getDependencies() );
    }

    private void copyNonExistingDependencies( Collection<Dependency> fromDependencies, List<Dependency> toDependencies ) {
        fromDependencies.stream()
            .filter( dependency -> !dependencyFoundWithVersion( dependency, toDependencies ) )
            .forEach( dependency -> toDependencies.add( dependency ) );
    }

    private Set<Dependency> getManagedDependencyImports( Model model ) {
        getConfigurationsFromParent( model );

        Set<Dependency> dependencies = new LinkedHashSet<>();
        dependencies.addAll(
            model.getDependencyManagement()
                .getDependencies()
                .stream()
                .filter( dependency -> !isPomImport( dependency ) )
                .collect( Collectors.toSet() ) );
        dependencies.addAll(
            model.getDependencyManagement()
                .getDependencies()
                .stream()
                .filter( dependency -> isPomImport( dependency ) )
                .flatMap( importDependency -> getManagedDependencies( importDependency ).stream() )
                .collect( Collectors.toSet() ) );
        return dependencies;
    }

    public Set<Dependency> getManagedDependencies( Dependency importDependency ) {
        return repository.readPom( GAV.fromDependency( importDependency ) )
            .map( model -> getManagedDependencyImports( model ) )
            .orElse( new LinkedHashSet<>() );

    }

    private boolean isPomImport( Dependency dependency ) {
        return "import".equals( dependency.getScope() ) && "pom".equals( dependency.getType() );
    }

    private List<Dependency> filterDependenciesAlreadyAdded( Set<Dependency> transitiveDependencies, Set<Dependency> dependencies ) {
        return transitiveDependencies
            .stream()
            .filter( dependency -> !dependencyExist( dependency, dependencies ) )
            .collect( Collectors.toList() );
    }

    private boolean dependencyExist( Dependency dependency, Set<Dependency> dependencies ) {
        return dependencies.stream()
            .filter( dep -> dep.getGroupId().equals( dependency.getGroupId() ) )
            .anyMatch( dep -> dep.getArtifactId().equals( dependency.getArtifactId() ) );
    }

    private boolean isExcluded( Dependency dependency, List<Exclusion> exclusions ) {
        if ( exclusions == null || exclusions.isEmpty() ) {
            return false;
        }
        return exclusions.stream()
            .filter( exclusion -> exclusion.getGroupId().equals( dependency.getGroupId() ) )
            .anyMatch( exclusion -> exclusion.getArtifactId().equals( dependency.getArtifactId() ) );
    }

    private boolean isValidScope( String scope, boolean includeTestScope ) {
        if ( StringUtils.isEmpty( scope ) ) {
            return true;
        }
        if ( "compile".equals( scope ) ) {
            return true;
        }
        if ( "test".equals( scope ) && includeTestScope ) {
            return true;
        }
        if ( "runtime".equals( scope ) ) {
            return true;
        }
        return false;
    }

    private Set<Dependency> getTransitiveDependencies( Dependency dependency ) {
        LOGGER.trace( "Following transitive dependencies of: {}", GAV.fromDependency( dependency ) );
        return repository.readPom( GAV.fromDependency( dependency ) )
            .map( model -> processPomStream( model, dependency.getExclusions(), false ) )
            .orElse( new LinkedHashSet<>() );
    }

    private void applyDependencyManagement( Model model, boolean allParentsLoaded ) {
        model.getDependencies()
            .stream()
            .forEach( dependency -> applyDependencyManagement( dependency, model ) );

        model.getDependencies()
            .stream()
            .forEach(
                dependency -> LOGGER.trace(
                    "dependency within {} after applying dependency management: {}:{}:{} scope:{}",
                    GAV.fromModel( model ),
                    dependency.getGroupId(),
                    dependency.getArtifactId(),
                    dependency.getVersion(),
                    dependency.getScope() ) );

        removeDoubleDependencies( model );
        detectUnresolvedVersions( model, allParentsLoaded );
    }

    private void applyDependencyManagement( Dependency dependency, Model model ) {
        copyVersionScopeAndExclusionsFromDependencyManagement( dependency, model );
    }

    private void detectUnresolvedVersions( Model model, boolean allParentsLoaded ) {
        model.getDependencies().stream().forEach( dependency -> detectUnresolvedVersions( dependency, model, allParentsLoaded ) );
    }

    private void detectUnresolvedVersions( Dependency resolveVersion, Model model, boolean allParentsLoaded ) {
        if ( StringUtils.isEmpty( resolveVersion.getVersion() ) ) {
            LOGGER.error(
                "After copying the versions from the dependency management the version for {}:{} in pom {} is still empty, model: {}",
                () -> resolveVersion.getGroupId(),
                () -> resolveVersion.getArtifactId(),
                () -> GAV.fromModel( model ),
                () -> new ModelIO().writeModelToString( model ) );
            if ( !allParentsLoaded ) {
                LOGGER.error(
                    "<-- This is likely caused by a inconsistency in one of the pom file referrring to an incorrect parent pom file, check the parent defined for {}",
                    GAV.fromModel( model ) );
                throw new DepencyResolvingException(
                    "An inconsistency has been found in the pom definitions, probably the parent of " + GAV.fromModel( model ) + " is not correctly defined" );
            }
            throw new DepencyResolvingException(
                GAV.fromModel( model ) + " has dependency issues, the version of " + resolveVersion.getGroupId() + ":" + resolveVersion.getArtifactId() +
                                                 " could not be defined, even after obtainining managed versions from the parents" );
        }
    }

    private void copyVersionScopeAndExclusionsFromDependencyManagement( Dependency resolveVersion, Model model ) {
        model.getDependencyManagement()
            .getDependencies()
            .stream()
            .filter( dependency -> dependency.getGroupId().equals( resolveVersion.getGroupId() ) )
            .filter( dependency -> dependency.getArtifactId().equals( resolveVersion.getArtifactId() ) )
            .findFirst()
            .ifPresent( dependency -> copyVersionScopeAndExclusionsTo( dependency, resolveVersion ) );
    }

    private void copyVersionScopeAndExclusionsTo( Dependency managedDependency, Dependency dependency ) {
        if ( !StringUtils.isEmpty( managedDependency.getVersion() ) ) {
            dependency.setVersion( managedDependency.getVersion() );
        }
        if ( StringUtils.isEmpty( dependency.getScope() ) ) {
            dependency.setScope( managedDependency.getScope() );
        }
        if ( managedDependency.getExclusions() != null && !managedDependency.getExclusions().isEmpty() ) {
            dependency.getExclusions().addAll( managedDependency.getExclusions() );
        }
    }

    private boolean getConfigurationsFromParent( Model model ) {
        if ( model.getDependencyManagement() == null ) {
            model.setDependencyManagement( new DependencyManagement() );
        }

        Parent parent = model.getParent();
        while ( parent != null ) {
            LOGGER.debug( "Reading from parent with gav: {}", GAV.fromParent( parent ) );
            Optional<Model> modelForParent = repository.readPom( GAV.fromParent( parent ) );
            if ( !modelForParent.isPresent() ) {
                return false;
            }
            Model parentModel = modelForParent.get();
            copyNonExistingManagedDependencies( parentModel, model );
            copyNonExistingDependencies( parentModel, model );
            copyNonExistingProperties( parentModel, model );
            parent = parentModel.getParent();
        }
        resolveAllPropertiesInAllSections( model );
        return true;
    }

    private void resolveAllPropertiesInAllSections( Model model ) {
        resolveAllPropertiesInDependencies( model.getDependencyManagement().getDependencies(), model );
        resolveAllPropertiesInDependencies( model.getDependencies(), model );
    }

    private void resolveAllPropertiesInDependencies( List<Dependency> dependencies, Model model ) {
        dependencies.stream().forEach( dependency -> resolveProperties( dependency, model ) );
    }

    private void copyNonExistingManagedDependencies( Model parentModel, Model model ) {
        if ( parentModel.getDependencyManagement() != null &&
             parentModel.getDependencyManagement().getDependencies() != null ) {
            parentModel.getDependencyManagement()
                .getDependencies()
                .stream()
                .map( dependency -> resolveGAV( dependency, parentModel ) )
                .filter( dependency -> !dependencyFoundWithVersion( dependency, model.getDependencyManagement().getDependencies() ) )
                .forEach( dependency -> model.getDependencyManagement().addDependency( dependency ) );
        }
    }

    private void copyNonExistingDependencies( Model parentModel, Model model ) {
        if ( parentModel.getDependencies() != null ) {
            parentModel.getDependencies()
                .stream()
                .map( dependency -> resolveGAV( dependency, parentModel ) )
                .filter( dependency -> !dependencyFoundWithVersion( dependency, model.getDependencies() ) )
                .forEach( dependency -> model.addDependency( dependency ) );
        }
    }

    private void copyNonExistingProperties( Model parentModel, Model model ) {
        if ( model.getProperties() != null ) {
            parentModel.getProperties()
                .entrySet()
                .stream()
                .filter( entry -> !model.getProperties().containsKey( entry.getKey() ) )
                .forEach( entry -> model.getProperties().put( entry.getKey(), entry.getValue() ) );
        }
    }

    private boolean dependencyFoundWithVersion( Dependency search, List<Dependency> dependencies ) {
        return dependencies.stream()
            .filter( dependency -> dependency.getGroupId().equals( search.getGroupId() ) )
            .filter( dependency -> dependency.getArtifactId().equals( search.getArtifactId() ) )
            .anyMatch( dependency -> !StringUtils.isEmpty( dependency.getVersion() ) );
    }

    private Dependency resolveGAV( Dependency dependency, Model parentModel ) {
        if ( pomUtils.isPropertyValue( dependency.getGroupId() ) ) {
            dependency.setGroupId( pomUtils.resolveProperty( dependency.getGroupId(), parentModel ) );
        }
        if ( pomUtils.isPropertyValue( dependency.getArtifactId() ) ) {
            dependency.setArtifactId( pomUtils.resolveProperty( dependency.getArtifactId(), parentModel ) );
        }
        if ( pomUtils.isPropertyValue( dependency.getVersion() ) ) {
            dependency.setVersion( pomUtils.resolveProperty( dependency.getVersion(), parentModel ) );
        }
        return dependency;
    }

    private void copyVersionsFromDependencyManagement( Set<Dependency> dependencies, List<Dependency> managedDependencies ) {
        dependencies.stream().forEach( dependency -> copyVersionIfManaged( dependency, managedDependencies ) );
    }

    private void copyVersionIfManaged( Dependency dependency, List<Dependency> managedDependencies ) {
        managedDependencies.stream()
            .filter( managedDependency -> managedDependency.getGroupId().equals( dependency.getGroupId() ) )
            .filter( managedDependency -> managedDependency.getArtifactId().equals( dependency.getArtifactId() ) )
            .forEach( managedDependency -> copyVersionScopeAndExclusionsTo( managedDependency, dependency ) );
    }

}
