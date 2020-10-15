package tools.jvm.mvn;

import org.cactoos.io.OutputTo;
import org.cactoos.io.TeeOutput;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings({"UnstableApiUsage", "ResultOfMethodCallIgnored"})
public class ArchiveTest {


    @Test
    public void exec() throws Exception {
        final Path tmp = Files.createTempDirectory("TMPDIRFORTEST");
        final Path dest = Files.createTempFile("ArchiveTest", ".tar");

        final Path art1 = tmp.resolve("some").resolve("artifact1").resolve("folder");
        final Path art1Jar = art1.resolve("xxx.pom");
        final Path art1Pom = art1.resolve("xxx.jar");

        final Path art2 = tmp.resolve("some").resolve("artifact2").resolve("folder1").resolve("folder2");
        final Path art2Pom = art2.resolve("yyy.pom");

        art1.toFile().mkdirs();
        art2.toFile().mkdirs();

        com.google.common.io.Files.touch(art1Jar.toFile());
        com.google.common.io.Files.touch(art1Pom.toFile());
        com.google.common.io.Files.touch(art2Pom.toFile());
        System.out.println(dest);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new Archive.Tar(tmp).exec(new OutputTo(baos));

        Assert.assertTrue(baos.size() > 0);
    }
}
