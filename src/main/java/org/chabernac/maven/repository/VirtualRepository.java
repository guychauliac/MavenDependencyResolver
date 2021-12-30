package org.chabernac.maven.repository;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.model.Model;
import org.chabernac.dependency.GAV;

public class VirtualRepository implements IRepository {
    private final Set<IRepository> repositories = new LinkedHashSet<>();

    @Override
    public Optional<Model> readPom(GAV gav) {
        return repositories
                .stream()
                .map(repository -> repository.readPom(gav))
                .filter(repository -> repository.isPresent())
                .findFirst()
                .orElse(Optional.empty());
    }

    public VirtualRepository addRepository(IRepository repository) {
        this.repositories.add(repository);
        return this;
    }

    @Override
    public boolean isWritable() {
        return repositories.stream()
                .anyMatch(repository -> repository.isWritable());
    }

    @Override
    public GAV store(Model model) {
        return repositories.stream()
                .filter(repository -> repository.isWritable())
                .map(repository -> repository.store(model))
                .findFirst()
                .orElseThrow(() -> new RepositoryException("This virtual repository does not have any underlying repo that is writable, storing models is not possible"));

    }

    @Override
    public Optional<Metadata> getMetaData(String groupId, String artifactId) {
        return repositories
                .stream()
                .map(repository -> repository.getMetaData(groupId, artifactId))
                .filter(repository -> repository.isPresent())
                .findFirst()
                .orElse(Optional.empty());  
    }

}
