package org.chabernac.dependency;

import java.util.function.BiFunction;

public class GetVersionsURL implements BiFunction<String, String, String> {
    private final String centralRepoURL;

    public GetVersionsURL(String centralRepoURL) {
        super();
        this.centralRepoURL = centralRepoURL;
    }

    @Override
    public String apply(String groupId, String artifactId) {
        StringBuilder builder = new StringBuilder();
        builder.append(centralRepoURL);
        if (!centralRepoURL.endsWith("/")) {
            builder.append("/");
        }
        builder.append(groupId.replace(".", "/"));
        builder.append("/");
        builder.append(artifactId);
        builder.append("/maven-metadata.xml");
        return builder.toString();
    }

}
