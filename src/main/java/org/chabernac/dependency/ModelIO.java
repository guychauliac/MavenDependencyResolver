package org.chabernac.dependency;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.chabernac.maven.repository.RepositoryException;

public class ModelIO implements IModelIO {
    @Override
    public Model getModelFromInputStream(InputStream inputStream) {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            return reader.read(inputStream);
        } catch (Exception e) {
            throw new RepositoryException("Could not read model from inputstream", e);
        }
    }

    @Override
    public void writeModelToStream(Model model, OutputStream outputStream) {
        MavenXpp3Writer writer = new MavenXpp3Writer();
        try {
            writer.write(outputStream, model);
        } catch (Exception e) {
            throw new RepositoryException("Could not write model to outputstream", e);
        }
    }

    @Override
    public String writeModelToString(Model model) {
        MavenXpp3Writer reader = new MavenXpp3Writer();
        try {
            ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
            reader.write(byteArrayOut, model);
            return byteArrayOut.toString();
        } catch (Exception e) {
            throw new RepositoryException("Could not write model to outputstream", e);
        }
    }

    @Override
    public Model getModelFromResource(String resource) {
        return getModelFromInputStream(getClass().getResourceAsStream(resource));
    }

    @Override
    public Model getModelFromString(String modelContent) {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        try {
            return reader.read(new StringReader(modelContent));
        } catch (Exception e) {
            throw new RepositoryException("Could not read model from inputstream", e);
        }
    }

    @Override
    public Metadata getMetaDataFromString(String metaContent) {
        MetadataXpp3Reader reader = new MetadataXpp3Reader();
        try {
            return reader.read(new StringReader(metaContent));
        } catch (Exception e) {
            throw new RepositoryException("Could not read metadata from stream", e);
        }
    }

    @Override
    public Metadata getMetaDataFromString(InputStream inputStream) {
        MetadataXpp3Reader reader = new MetadataXpp3Reader();
        try {
            return reader.read(inputStream);
        } catch (Exception e) {
            throw new RepositoryException("Could not read metadata from stream", e);
        }
    }

    @Override
    public void writeMetadataToStream(Metadata metadata, FileOutputStream output) {
        MetadataXpp3Writer writer = new MetadataXpp3Writer();
        try {
            writer.write(output, metadata);
        } catch (Exception e) {
            throw new RepositoryException("Could not write metadata to outputstream", e);
        }

    }
}
