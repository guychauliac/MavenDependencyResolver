package org.chabernac.maven.repository;

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

public class RemoteRepository implements IRepository {
    public static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";
    private static final Logger LOGGER = LogManager.getLogger(RemoteRepository.class);

    private final Function<GAV, String> getMavenURL;
    private final BiFunction<String, String, String> getVersionURL;
    private final IModelIO modelIO;
    private final IRemoteRepositoryAdapter remoteRepositoryAdapter;

    public RemoteRepository() {
        this(MAVEN_CENTRAL);
    }

    public RemoteRepository(String repository) {
        this(new ModelIO(), new RemoteRepositoryAdapter(new DefaultRemoteRepositoryRequestBuilder()), new GetMavenRepoURL(repository), new GetVersionsURL(repository));
    }

    public RemoteRepository(String repository,
            IRemoteRepositoryRequestBuilder remoteRepositoryRequestBuilder) {
        this(new ModelIO(), new RemoteRepositoryAdapter(remoteRepositoryRequestBuilder), new GetMavenRepoURL(repository), new GetVersionsURL(repository));
    }

    RemoteRepository(IModelIO modelIO,
            IRemoteRepositoryAdapter remoteRepositoryAdapter,
            Function<GAV, String> getMavenURL,
            BiFunction<String, String, String> getVersionURL) {
        this.getMavenURL = getMavenURL;
        this.modelIO = modelIO;
        this.remoteRepositoryAdapter = remoteRepositoryAdapter;
        this.getVersionURL = getVersionURL;
    }

    @Override
    public Optional<Model> readPom(GAV gav) {
        String endPoint = getMavenURL.apply(gav);
        LOGGER.debug("Resolving pom.xml from '{}'", endPoint);

        Optional<String> pomStream = remoteRepositoryAdapter.call(endPoint);

        if (!pomStream.isPresent()) {
            return Optional.empty();
        }

        return pomStream.map(pomString -> modelIO.getModelFromString(pomString));
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public GAV store(Model model) {
        throw new UnsupportedOperationException("store is not supported on this repository");
    }

    @Override
    public Optional<Metadata> getMetaData(String groupId, String artifactId) {
        String endPoint = getVersionURL.apply(groupId, artifactId);
        Optional<String> metaStream = remoteRepositoryAdapter.call(endPoint);
        if (!metaStream.isPresent()) {
            return Optional.empty();
        }
        return Optional.of(modelIO.getMetaDataFromString(metaStream.get()));
    }

}
