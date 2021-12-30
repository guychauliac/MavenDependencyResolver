package org.chabernac.maven.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.model.Model;
import org.chabernac.dependency.GAV;

public class InMemoryCachingRepository implements IRepository {
    private final IRepository repository;

    private final Map<GAV, Optional<Model>> cache = new HashMap<>();
    private final Map<String, Optional<Metadata>> metacache = new HashMap<>();

    public InMemoryCachingRepository(IRepository repository) {
        super();
        this.repository = repository;
    }

    @Override
    public Optional<Model> readPom(GAV gav) {
        if (cache.containsKey(gav)) {
            return cache.get(gav);
        }
        Optional<Model> model = repository.readPom(gav);
        cache.put(gav, model);
        return model;
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
        String key = groupId + "-" + artifactId;
        if (metacache.containsKey(key)) {
            return metacache.get(key);
        }
        Optional<Metadata> metadata = repository.getMetaData(groupId, artifactId);
        metacache.put(key, metadata);
        return metadata;
    }
}
