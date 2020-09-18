package tools.jvm.mvn;

import com.google.common.io.ByteSource;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class OutputTest {

    @Test
    public void templating() throws IOException {
        Project p = new Project() {
            @Override
            public Path pom() {
                try {
                    return Files.createTempFile("pom", ".xml");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }.lazy();

        String pom = "<project>\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.mavenizer.gid</groupId>\n" +
                "    <artifactId>xyz</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "</project>";

        Files.write(p.pom(), pom.getBytes());

        final String src = new Output.Paths("{{artifactId}}-{{version}}.jar", "jar", p).src();
        Assert.assertEquals(src, "xyz-1.0.0-SNAPSHOT.jar");
    }
}
