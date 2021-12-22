package org.chabernac.dependency;

import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.model.Dependency;
import org.chabernac.maven.repository.FileCachingRepository;
import org.chabernac.maven.repository.InMemoryCachingRepository;
import org.chabernac.maven.repository.LocalInMemoryRepository;
import org.chabernac.maven.repository.RemoteRepository;
import org.chabernac.maven.repository.RemoteRepositoryForTest;
import org.chabernac.maven.repository.VirtualRepository;
import org.junit.Assert;
import org.junit.Test;

public class ResolveDependenciesTest {
    private ResolveDependencies resolveDependencies = new ResolveDependencies(
        new VirtualRepository()
            .addRepository(
                new InMemoryCachingRepository(
                    new FileCachingRepository( Paths.get( System.getProperty( "java.io.tmpdir" ), "pomcache" ),
                        new RemoteRepository( RemoteRepositoryForTest.REPO ) ) ) )
            .addRepository( new LocalInMemoryRepository() ) );

    @Test
    public void resolveDependencies() {
        Set<Dependency> dependencies = resolveDependencies.getDependencies( getClass().getResourceAsStream( "/pom.xml" ) );

        List<String> result = dependencies.stream().map( dependency -> GAV.fromDependency( dependency ).toString() ).collect( Collectors.toList() );

        Assert.assertEquals( 10, dependencies.size() );
        Assert.assertTrue( result.contains( "GAV [groupId=org.apiguardian, artifactId=apiguardian-api, version=1.0.0]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.apache.commons, artifactId=commons-lang3, version=3.4]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.junit.jupiter, artifactId=junit-jupiter-api, version=5.4.0]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.junit.jupiter, artifactId=junit-jupiter-engine, version=5.4.0]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.junit.platform, artifactId=junit-platform-commons, version=1.4.0]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.junit.platform, artifactId=junit-platform-engine, version=1.4.0]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.apache.maven, artifactId=maven-model, version=3.3.9]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.apache.maven, artifactId=maven-settings, version=3.8.4]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.opentest4j, artifactId=opentest4j, version=1.1.1]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.codehaus.plexus, artifactId=plexus-utils, version=3.0.22]" ) );
    }
    
    @Test
    public void resolveDependenciesWithManagedDependency() {
        Set<Dependency> dependencies = resolveDependencies.getDependencies( getClass().getResourceAsStream( "/pom-override-dependency-with-management.pom.xml" ) );

        List<String> result = dependencies.stream().map( dependency -> GAV.fromDependency( dependency ).toString() ).collect( Collectors.toList() );

        Assert.assertEquals( 10, dependencies.size() );
        Assert.assertTrue( result.contains( "GAV [groupId=org.apiguardian, artifactId=apiguardian-api, version=1.0.0]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.apache.commons, artifactId=commons-lang3, version=3.3]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.junit.jupiter, artifactId=junit-jupiter-api, version=5.4.0]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.junit.jupiter, artifactId=junit-jupiter-engine, version=5.4.0]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.junit.platform, artifactId=junit-platform-commons, version=1.4.0]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.junit.platform, artifactId=junit-platform-engine, version=1.4.0]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.apache.maven, artifactId=maven-model, version=3.3.9]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.apache.maven, artifactId=maven-settings, version=3.8.4]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.opentest4j, artifactId=opentest4j, version=1.1.1]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.codehaus.plexus, artifactId=plexus-utils, version=3.0.22]" ) );
    }

    @Test
    public void resolveDependenciesWithImport() {
        Set<Dependency> dependencies = resolveDependencies.getDependencies( getClass().getResourceAsStream( "/commons-io-2.11.0.pom.xml" ) );

        List<String> result = dependencies.stream().map( dependency -> GAV.fromDependency( dependency ).toString() ).collect( Collectors.toList() );

        Assert.assertEquals( 28, dependencies.size() );
        Assert.assertTrue( result.contains( "GAV [groupId=org.apiguardian, artifactId=apiguardian-api, version=1.1.0]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=net.bytebuddy, artifactId=byte-buddy, version=1.11.3]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=net.bytebuddy, artifactId=byte-buddy-agent, version=1.11.3]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.checkerframework, artifactId=checker-compat-qual, version=2.5.5]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.apache.commons, artifactId=commons-lang3, version=3.12.0]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.apache.commons, artifactId=commons-math3, version=3.2]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=com.google.errorprone, artifactId=error_prone_annotations, version=2.3.4]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=com.google.guava, artifactId=failureaccess, version=1.0.1]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=com.google.guava, artifactId=guava, version=30.1-android]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=com.google.j2objc, artifactId=j2objc-annotations, version=1.3]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=com.google.jimfs, artifactId=jimfs, version=1.2]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.openjdk.jmh, artifactId=jmh-core, version=1.32]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.openjdk.jmh, artifactId=jmh-generator-annprocess, version=1.32]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=net.sf.jopt-simple, artifactId=jopt-simple, version=4.6]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=com.google.code.findbugs, artifactId=jsr305, version=3.0.2]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.junit.jupiter, artifactId=junit-jupiter, version=5.7.2]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.junit.jupiter, artifactId=junit-jupiter-api, version=5.7.2]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.junit.jupiter, artifactId=junit-jupiter-engine, version=5.7.2]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.junit.jupiter, artifactId=junit-jupiter-params, version=5.7.2]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.junit-pioneer, artifactId=junit-pioneer, version=1.4.2]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.junit.platform, artifactId=junit-platform-commons, version=1.7.2]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.junit.platform, artifactId=junit-platform-engine, version=1.7.2]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.junit.platform, artifactId=junit-platform-launcher, version=1.7.2]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=com.google.guava, artifactId=listenablefuture, version=9999.0-empty-to-avoid-conflict-with-guava]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.mockito, artifactId=mockito-core, version=3.11.2]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.mockito, artifactId=mockito-inline, version=3.11.2]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.objenesis, artifactId=objenesis, version=3.2]" ) );
        Assert.assertTrue( result.contains( "GAV [groupId=org.opentest4j, artifactId=opentest4j, version=1.2.0]" ) );
    }

    @Test
    public void resolveDependenciesMultiModule() {
        Set<Dependency> dependencies = resolveDependencies.getDependencies(
            getClass().getResourceAsStream( "/parent/pom.xml" ),
            getClass().getResourceAsStream( "/module1/pom.xml" ),
            getClass().getResourceAsStream( "/module2/pom.xml" ) );

        List<String> result = dependencies.stream().map( dependency -> GAV.fromDependency( dependency ).toString() ).collect( Collectors.toList() );

        Assert.assertEquals( 26, result.size() );
        // TODO add assertions for expected library
        System.out.println( "Add assertions for each library" );
    }
}
