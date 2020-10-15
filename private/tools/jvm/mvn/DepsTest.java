package tools.jvm.mvn;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.io.CharSource;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;

@RunWith(BlockJUnit4ClassRunner.class)
public class DepsTest {

    @Test
    public void commonPrefixSingle() {
        Deps col = getPaths(
                ImmutableList.of(
                        Paths.get("/foo/bar/baz/A.txt")
                )
        );

        final Path path = col.resolveCommonPrefix();
        Assert.assertEquals(Paths.get("foo/bar/baz"), path);
    }

    @Test
    public void commonPrefix() {
        Deps col = getPaths(ImmutableList.of(
                Paths.get("/foo/bar/baz/A.txt"),
                Paths.get("/foo/bar/baz/B.txt"),
                Paths.get("/foo/bar/jaz/C.txt"),
                Paths.get("/foo/bar/jaz/tmp/tmp$1.txt"),
                Paths.get("/foo/bar/BUILD")
        ));
        final Path path = col.resolveCommonPrefix();
        Assert.assertEquals(Paths.get("foo/bar"), path);
    }


    @Test
    public void load() {
        Deps col = new Deps.Manifest(CharSource.wrap(
                "{\"path\": \"/foo/bar/baz/A.java\"}'\n" +
                        "{\"path\": \"/foo/bar/baz/B.java?scope=provided\"}\n" +
                        "{\"path\": \"/foo/bar/jazz/C.java?scope=provided\"}\n" +
                        "{\"path\": \"/foo/bar/roo/X.java\"}")
        );
        final Path path = col.resolveCommonPrefix();
        Assert.assertEquals("col="+col, Paths.get("foo/bar"), path);
        Assert.assertEquals("col="+col, Paths.get("/foo/bar/baz/A.java"), col.paths().findFirst().get());
    }



    @Test
    public void url() {
        Deps col = new Deps.Manifest(CharSource.wrap(
                "{\"path\": \"bazel-out/darwin-fastbuild/bin/tests/integration/lib/src/com/mavenizer/examples/subliby/libsubliby.jar\"}"
        ));

        Assert.assertEquals("col="+col, Paths.get(
                "bazel-out/darwin-fastbuild/bin/tests/integration/lib/src/com/mavenizer/examples/subliby/libsubliby.jar"), col.paths().findFirst().get());
    }

    private static Deps getPaths(Collection<Path> of) {
        return new Deps() {
            @Override
            public Iterator<DepArtifact> iterator() {
                return Iterators.transform(of.iterator(), DepArtifact::new);
            }
        };
    }
}
