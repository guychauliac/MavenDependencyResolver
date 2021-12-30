package org.chabernac.maven.repository;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.model.Model;
import org.chabernac.dependency.GAV;
import org.chabernac.dependency.GetMavenRepoURL;
import org.chabernac.dependency.GetVersionsURL;
import org.chabernac.dependency.IModelIO;
import org.chabernac.dependency.ModelIO;

public class FileCachingRepository implements IRepository {
    private static final Logger LOGGER = LogManager.getLogger(FileCachingRepository.class);

    private final IRepository repository;
    private final Function<GAV, String> getMavenRepoPath;
    private final BiFunction<String, String, String> getMavenMetaRepoPath;
    private final IModelIO modelIO;

    public FileCachingRepository(Path basePath,
            IRepository repository) {
        this(repository, basePath, new ModelIO());
    }

    FileCachingRepository(IRepository repository,
            Path basePath,
            IModelIO modelIO) {
        super();
        this.repository = repository;
        this.getMavenRepoPath = new GetMavenRepoURL(basePath.toString());
        this.getMavenMetaRepoPath = new GetVersionsURL(basePath.toString());
        this.modelIO = modelIO;
    }

    @Override
    public Optional<Model> readPom(GAV gav) {
        Path path = Paths.get(getMavenRepoPath.apply(gav));
        try {
            if (Files.exists(path)) {
                LOGGER.trace("Reading pom from '{}'", path.toFile());
                try (FileInputStream input = new FileInputStream(path.toFile())) {
                    return Optional.of(modelIO.getModelFromInputStream(input));
                }
            }

            Optional<Model> modelOptional = repository.readPom(gav);

            if (modelOptional.isPresent()) {
                Files.createDirectories(path.getParent());
                LOGGER.trace("Writing pom from '{}'", path.toFile());
                try (FileOutputStream output = new FileOutputStream(path.toFile())) {
                    modelIO.writeModelToStream(modelOptional.get(), output);
                }
            }

            return modelOptional;
        } catch (IOException e) {
            throw new RepositoryException("Could not read/write to path: '" + path + "'", e);
        }

    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public GAV store(Model model) {
        throw new UnsupportedOperationException("Store not supported in this repository");
    }

    @Override
    public Optional<Metadata> getMetaData(String groupId, String artifactId) {
        Path path = Paths.get(getMavenMetaRepoPath.apply(groupId, artifactId));
        try {
            if (Files.exists(path)) {
                LOGGER.trace("Reading metadata from '{}'", path.toFile());
                try (FileInputStream input = new FileInputStream(path.toFile())) {
                    return Optional.of(modelIO.getMetaDataFromString(input));
                }
            }

            Optional<Metadata> metaOptional = repository.getMetaData(groupId, artifactId);

            if (metaOptional.isPresent()) {
                Files.createDirectories(path.getParent());
                LOGGER.trace("Writing metadata from '{}'", path.toFile());
                try (FileOutputStream output = new FileOutputStream(path.toFile())) {
                    modelIO.writeMetadataToStream(metaOptional.get(), output);
                }
            }

            return metaOptional;
        } catch (IOException e) {
            throw new RepositoryException("Could not read/write to path: '" + path + "'", e);
        }
    }


}
