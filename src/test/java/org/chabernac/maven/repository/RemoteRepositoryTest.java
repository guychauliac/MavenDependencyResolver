package org.chabernac.maven.repository;

import java.io.IOException;
import java.util.Optional;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.model.Model;
import org.chabernac.dependency.GAV;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import okhttp3.Request;

@RunWith(MockitoJUnitRunner.class)
public class RemoteRepositoryTest {
    private RemoteRepository remoteRepository = new RemoteRepository(RemoteRepositoryForTest.REPO);

    @Mock
    private IRemoteRepositoryRequestBuilder remoteRepositoryRequestBuilder;

    @Test
    public void readPom() throws IOException {
        Optional<Model> result = remoteRepository.readPom(new GAV("com.squareup.okhttp3", "okhttp", "4.9.3"));

        Assert.assertTrue(result.isPresent());
        System.out.println(result.get());
    }

    @Test
    public void readPomWithCustomRequestBuilder() throws IOException {
        remoteRepository = new RemoteRepository(RemoteRepositoryForTest.REPO, remoteRepositoryRequestBuilder);
        String url = RemoteRepositoryForTest.REPO + "/com/squareup/okhttp3/okhttp/4.9.3/okhttp-4.9.3.pom";
        Mockito.when(remoteRepositoryRequestBuilder.apply(url)).thenReturn(
                new Request.Builder()
                        .url(url)
                        .build());

        Optional<Model> result = remoteRepository.readPom(new GAV("com.squareup.okhttp3", "okhttp", "4.9.3"));

        Assert.assertTrue(result.isPresent());
        Mockito.verify(remoteRepositoryRequestBuilder, Mockito.times(1)).apply(url);
    }

    @Test
    public void readPomApacheMaven() throws IOException, XmlPullParserException {
        Optional<Model> result = remoteRepository.readPom(new GAV("org.apache.maven", "maven", "3.3.9"));

        Assert.assertTrue(result.isPresent());
        System.out.println(result.get());
    }


    @Test
    public void readMetaDataApacheMaven() throws IOException, XmlPullParserException {
        Optional<Metadata> result = remoteRepository.getMetaData("org.apache.maven", "maven");

        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get().getVersioning().getVersions().size() >= 62);
    }
}
