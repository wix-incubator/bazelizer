package tools.jvm.mvn;

import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("UnstableApiUsage")
public class ActsTest {

    File tmpWorkDir ;

    @Before
    public void tmp() {
        tmpWorkDir = Files.createTempDir();
    }

    @After
    public void rm() {
        //noinspection ResultOfMethodCallIgnored
        tmpWorkDir.delete();
    }

    @Test
    public void generatePom() throws IOException {
        String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project>\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId> {{ groupId }} </groupId>\n" +
                "    <artifactId> {{ artifactId }} </artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "    <dependencies>\n" +
                "        {{#deps}}\n" +
                "        <dependency>\n" +
                "            <groupId>{{groupId}}</groupId>\n" +
                "            <artifactId>{{artifactId}}</artifactId>\n" +
                "            <version>{{version}}</version>\n" +
                "        </dependency>\n" +
                "        {{/deps}}\n" +
                "    </dependencies>\n" +
                "</project>";

        Project p = Project.builder()
                .artifactId("AAAA")
                .groupId("BBBB")
                .workDir(tmpWorkDir.toPath())
                .pomTemplate(ByteSource.wrap(pom.getBytes()))
                .deps(Lists.newArrayList(new Dep.Simple(null, "xyz", "xyz-aaa", "1.0")))
                .build();

        final Act.Iterative act = new Act.Iterative(
                new Acts.POM()
        );
        final Project accept = act.accept(p);

        XML xml = new XMLDocument(accept.pom().toFile());
        Assert.assertEquals(xml.xpath("//project/groupId/text()").get(0).trim(), "BBBB");
        Assert.assertEquals(xml.xpath("//project/artifactId/text()").get(0).trim(), "AAAA");
        Assert.assertEquals(xml.xpath("//project/dependencies/dependency/groupId/text()"), Lists.newArrayList("xyz"));
        Assert.assertEquals(xml.xpath("//project/dependencies/dependency/artifactId/text()"), Lists.newArrayList("xyz-aaa"));
        Assert.assertEquals(xml.xpath("//project/dependencies/dependency/version/text()"), Lists.newArrayList("1.0"));
    }


    @Test
    public void install() throws IOException {
        final Act.Iterative act = new Act.Iterative(
                new Acts.Deps()
        );
        Path jar = java.nio.file.Files.createTempFile("jar", "jar");

        Files.touch(jar.toFile());
        jar.toFile().deleteOnExit();

        final Project p = Project.builder()
                .workDir(tmpWorkDir.toPath())
                .m2Home(tmpWorkDir.toPath())
                .deps(Lists.newArrayList(
                        new Dep.Simple(jar.toFile(), "xyz.com.baz", "xyz-aaa", "1.0")))
                .build();

        act.accept(p);

        final Path resolve = tmpWorkDir.toPath().resolve("repository/xyz/com/baz/xyz-aaa/1.0/");
        Assert.assertTrue(resolve.resolve("xyz-aaa-1.0.jar").toFile().exists());
        Assert.assertTrue(resolve.resolve("xyz-aaa-1.0.pom").toFile().exists());
    }

    @Test
    public void settings() {
        final Act.Iterative act = new Act.Iterative(
                new Acts.SettingsXml().userLocalCache(Paths.get("~/.m2/repository"))
        );

        final Project p = Project.builder()
                .m2Home(tmpWorkDir.toPath())
                .build();

        act.accept(p);
    }
}
