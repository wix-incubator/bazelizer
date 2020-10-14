package tools.jvm.mvn;

import com.google.common.collect.ImmutableList;
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
public class FilePathsTest {

    @Test
    public void commonPrefixSingle() {
        FilePaths col = getPaths(
                ImmutableList.of(
                        Paths.get("/foo/bar/baz/A.txt")
                )
        );

        final Path path = col.resolveCommonPrefix();
        Assert.assertEquals(Paths.get("foo/bar/baz"), path);
    }

    @Test
    public void commonPrefix() {
        FilePaths col = getPaths(ImmutableList.of(
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
        FilePaths col = new FilePaths.Manifest(CharSource.wrap(
                "'/foo/bar/baz/A.java'\n" +
                "'/foo/bar/baz/B.java'\n" +
                "'/foo/bar/jazz/C.java'\n" +
                "'/foo/bar/roo/X.java'"));
        final Path path = col.resolveCommonPrefix();
        Assert.assertEquals("col="+col,Paths.get("foo/bar"), path);
    }

    private static FilePaths getPaths(Collection<Path> of) {
        return new FilePaths() {
            @Override
            public Iterator<Path> iterator() {
                return of.iterator();
            }
        };
    }
}
