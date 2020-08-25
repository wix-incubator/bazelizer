package tools.jvm.mvn;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

@RunWith(BlockJUnit4ClassRunner.class)
public class PathsCollectionTest {

    @Test
    public void commonPrefixSingle() {
        PathsCollection col = getPaths(
                ImmutableList.of(
                        Paths.get("/foo/bar/baz/A.txt")
                )
        );

        final Optional<Path> path = col.commonPrefix();
        Assert.assertEquals(Optional.of(Paths.get("foo/bar/baz")), path);
    }

    @Test
    public void commonPrefix() {
        PathsCollection col = getPaths(ImmutableList.of(
                Paths.get("/foo/bar/baz/A.txt"),
                Paths.get("/foo/bar/baz/B.txt"),
                Paths.get("/foo/bar/jaz/C.txt"),
                Paths.get("/foo/bar/jaz/tmp/tmp$1.txt"),
                Paths.get("/foo/bar/BUILD")
        ));
        final Optional<Path> path = col.commonPrefix();
        Assert.assertEquals(Optional.of(Paths.get("foo/bar")), path);
    }


    @Test
    public void load() {
        PathsCollection col = new PathsCollection.Manifest(CharSource.wrap(
                "'/foo/bar/baz/A.java'\n" +
                "'/foo/bar/baz/B.java'\n" +
                "'/foo/bar/jazz/C.java'\n" +
                "'/foo/bar/roo/X.java'"));
        final Optional<Path> path = col.commonPrefix();
        Assert.assertEquals("col="+col, Optional.of(Paths.get("foo/bar")), path);
    }

    private static PathsCollection getPaths(Collection<Path> of) {
        return new PathsCollection() {
            @Override
            public Iterator<Path> iterator() {
                return of.iterator();
            }
        };
    }
}
