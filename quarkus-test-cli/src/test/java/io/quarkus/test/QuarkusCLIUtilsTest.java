package io.quarkus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.junit.jupiter.api.Test;

import io.quarkus.test.util.QuarkusCLIUtils;

public class QuarkusCLIUtilsTest {

    @Test
    public void testGetQuarkusAppVersion() {
        DefaultArtifactVersion communityVersion3Dots = QuarkusCLIUtils.getQuarkusAppVersion("3.15.3");
        assertEquals("3.15.3", communityVersion3Dots.toString());
        assertEquals(3, communityVersion3Dots.getMajorVersion());
        assertEquals(15, communityVersion3Dots.getMinorVersion());
        assertEquals(3, communityVersion3Dots.getIncrementalVersion());
        DefaultArtifactVersion productVersion3Dots = QuarkusCLIUtils.getQuarkusAppVersion("3.15.4.redhat-00001");
        assertEquals("3.15.4.redhat-00001", productVersion3Dots.toString());
        assertEquals(3, productVersion3Dots.getMajorVersion());
        assertEquals(15, productVersion3Dots.getMinorVersion());
        assertEquals(4, productVersion3Dots.getIncrementalVersion());
        assertTrue(productVersion3Dots.toString().contains("redhat"));
        DefaultArtifactVersion communityVersion4Dots = QuarkusCLIUtils.getQuarkusAppVersion("3.15.3.1");
        assertEquals("3.15.3", communityVersion4Dots.toString());
        assertEquals(3, communityVersion4Dots.getMajorVersion());
        assertEquals(15, communityVersion4Dots.getMinorVersion());
        assertEquals(3, communityVersion4Dots.getIncrementalVersion());
        DefaultArtifactVersion productVersion4Dots = QuarkusCLIUtils.getQuarkusAppVersion("3.15.3.1.redhat-00001");
        assertEquals("3.15.3.redhat-00001", productVersion4Dots.toString());
        assertEquals(3, productVersion4Dots.getMajorVersion());
        assertEquals(15, productVersion4Dots.getMinorVersion());
        assertEquals(3, productVersion4Dots.getIncrementalVersion());
        assertTrue(productVersion4Dots.toString().contains("redhat"));
    }
}
