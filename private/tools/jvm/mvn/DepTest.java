package tools.jvm.mvn;

import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public class DepTest {

    private Dep.DigestCoords getDep(String s) {
        return new Dep.DigestCoords(Paths.get(s));
    }

    @Test
    public void art() {
        Dep dep = new Dep.Archived(
                Paths.get("/tmp/zzz"),
                "com/mavenizer/examples/api/myapi-single/1.0.0-SNAPSHOT/myapi-single-1.0.0-SNAPSHOT.jar"
        );

        Assert.assertEquals("com.mavenizer.examples.api", dep.groupId());
        Assert.assertEquals("myapi-single", dep.artifactId());
        Assert.assertEquals("1.0.0-SNAPSHOT", dep.version());

        final Path relative = dep.relativeTo(Paths.get("/tmp/repo"));
        Assert.assertEquals("/tmp/repo/com/mavenizer/examples/api/myapi-single/1.0.0-SNAPSHOT", relative.toString());

    }

    @Test
    public void digest() {
        Dep dep = getDep("/tmp/external/com_github_matsluni_aws_spi_akka_http_2_12.jar");
        Assert.assertEquals("io.bazelbuild.3bf7d0e72d22040ee21784104f17d56d", dep.groupId());
        Assert.assertEquals("com_github_matsluni_aws_spi_akka_http_2_12", dep.artifactId());
        Assert.assertEquals("rev-3bf7d0e", dep.version());
    }

    @Test
    public void urlUnsafe() {
        Dep dep = getDep("/tmp/external/app=._profile.jar");
        Assert.assertEquals("io.bazelbuild.fdc7f66df6dd5bff1eb699e10753ebcd", dep.groupId());
        Assert.assertEquals("app_._profile", dep.artifactId());
        Assert.assertEquals("rev-fdc7f66", dep.version());
    }

    @Test
    public void eq() {
        String str = "/Users/bohdans/Projects/tool/foo/bar/searcher/component/filter/CanonicalizeQuerySupport.java";
        Dep dep1 = getDep(str);
        Dep dep2 = getDep(str);
        Assert.assertEquals(dep1, dep2);
    }
}
