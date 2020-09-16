package tools.jvm.mvn;

import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Paths;

public class DepTest {

    @Test
    public void digest() {
        Dep dep = new Dep.DigestCoords(Paths.get("/tmp/external/com_github_matsluni_aws_spi_akka_http_2_12.jar"));
        Assert.assertEquals("io.bazelbuild.3bf7d0e72d22040ee21784104f17d56d", dep.groupId());
        Assert.assertEquals("com_github_matsluni_aws_spi_akka_http_2_12", dep.artifactId());
        Assert.assertEquals("rev-3bf7d0e", dep.version());
    }

    @Test
    public void urlUnsafe() {
        Dep dep = new Dep.DigestCoords(Paths.get("/tmp/external/app=._profile.jar"));
        Assert.assertEquals("io.bazelbuild.fdc7f66df6dd5bff1eb699e10753ebcd", dep.groupId());
        Assert.assertEquals("app_._profile", dep.artifactId());
        Assert.assertEquals("rev-fdc7f66", dep.version());
    }

    @Test
    public void eq() {
        String str = "/Users/bohdans/Projects/tool/foo/bar/searcher/component/filter/CanonicalizeQuerySupport.java";
        Dep dep1 = new Dep.DigestCoords(Paths.get(str));
        Dep dep2 = new Dep.DigestCoords(Paths.get(str));
        Assert.assertEquals(dep1, dep2);
    }
}
