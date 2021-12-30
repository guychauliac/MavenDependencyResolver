package org.chabernac.maven.repository;

import java.util.Optional;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.model.Model;
import org.chabernac.dependency.GAV;

public interface IRepository {
    public Optional<Model> readPom( GAV gav );

    public boolean isWritable();

    public GAV store( Model model );
    
    public Optional<Metadata> getMetaData(String groupId, String artifactId);
}
