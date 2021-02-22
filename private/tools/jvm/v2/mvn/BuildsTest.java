package tools.jvm.v2.mvn;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BuildsTest {


    final String pomStr = "<project >\n" +
            "\n" +
            "\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>com.mavenizer.examples-single.lib</groupId>\n" +
            "    <artifactId>myapi-single</artifactId>\n" +
            "    <version>1.0.0</version>\n" +
            "\n" +
            "\n" +
            "    <dependencies>\n" +
            "    </dependencies>\n" +
            "\n" +
            "    <build>\n" +
            "        <plugins>\n" +
            "            <plugin>\n" +
            "                <groupId>org.apache.maven.plugins</groupId>\n" +
            "                <artifactId>maven-compiler-plugin</artifactId>\n" +
            "                <configuration>\n" +
            "                    <source>1.6</source>\n" +
            "                    <target>1.6</target>\n" +
            "                </configuration>\n" +
            "            </plugin>\n" +
            "        </plugins>\n" +
            "    </build>\n" +
            "</project>";

    @Test
    public void build() throws IOException {
        final Builds pomFiles = new Builds();
        final Path aFile = Files.createTempFile("pom", ".xml");

        Files.write(aFile, pomStr.getBytes());
        pomFiles.registerFile(new Builds.BuildInfo(aFile));

        final Builds.BuildsOrder builds = pomFiles.travers();
        System.out.println(builds);

        builds.each(pf -> {
            final File location = pf.pomFile().persisted(false);
            System.out.println(location);

            final Pom pom = pf.pomFile().pom();
            System.out.println(pom.asString());
        });

    }




}
