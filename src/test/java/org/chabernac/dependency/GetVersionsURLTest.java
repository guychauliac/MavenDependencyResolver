package org.chabernac.dependency;

import org.junit.Assert;
import org.junit.Test;

public class GetVersionsURLTest {
    private GetVersionsURL getVersionsURL = new GetVersionsURL("https://repo1.maven.org/maven2/");

    @Test
    public void apply() {
        String result = getVersionsURL.apply("org.mockito", "mockito-all");

        Assert.assertEquals("https://repo1.maven.org/maven2/org/mockito/mockito-all/maven-metadata.xml", result);
    }
}
