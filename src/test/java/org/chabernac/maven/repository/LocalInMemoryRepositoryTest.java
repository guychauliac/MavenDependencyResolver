package org.chabernac.maven.repository;

import java.util.Optional;
import org.apache.maven.model.Model;
import org.chabernac.dependency.GAV;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith( MockitoJUnitRunner.class )
public class LocalInMemoryRepositoryTest {
    private LocalInMemoryRepository repository = new LocalInMemoryRepository();

    @Mock
    private Model                   model;

    @Test
    public void addModel() {
        Mockito.when( model.getGroupId() ).thenReturn( "groupId" );
        Mockito.when( model.getArtifactId() ).thenReturn( "artifactId" );
        Mockito.when( model.getVersion() ).thenReturn( "version" );

        repository.store( model );

        Optional<Model> result = repository.readPom( new GAV( "groupId", "artifactId", "version" ) );

        Assert.assertTrue( result.isPresent() );
        Assert.assertSame( model, result.get() );
    }
}
