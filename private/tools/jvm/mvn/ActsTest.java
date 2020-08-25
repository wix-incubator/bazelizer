package tools.jvm.mvn;

import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


@SuppressWarnings("UnstableApiUsage")
public class ActsTest {

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

        class Tmp implements Project {
            Path tmpWorkDir = Files.createTempDir().toPath();
            Tmp() throws IOException {
                tmpWorkDir.toFile().deleteOnExit();
            }

            @Override
            public String artifactId() {
                return "AAAA";
            }
            @Override
            public String groupId() {
                return "BBBB";
            }
            @Override
            public Path workDir() {
                return tmpWorkDir;
            }
            @Override
            public ByteSource pomXmlTpl() {
                return ByteSource.wrap(pom.getBytes());
            }
            @Override
            public Iterable<Dep> deps() {
                return Lists.newArrayList(new Dep.Simple(null, "xyz", "xyz-aaa", "1.0"));
            }
        }

        final Act.Memento act = new Act.Memento(
                new Acts.PomMustache()
        );
        final Project accept = act.accept(new Tmp());
        final List<Args.KW> kws = accept.args().toKW();
        final Optional<Args.KW> pomFileArg = kws.stream().filter(x -> x.key.equals("f")).findFirst();
        Assert.assertThat("was: " + kws, pomFileArg.isPresent(), Matchers.is(true));

        XML xml = new XMLDocument(new File(pomFileArg.get().value));
        Assert.assertEquals(xml.xpath("//project/groupId/text()").get(0).trim(), "BBBB");
        Assert.assertEquals(xml.xpath("//project/artifactId/text()").get(0).trim(), "AAAA");
        Assert.assertEquals(xml.xpath("//project/dependencies/dependency/groupId/text()"), Lists.newArrayList("xyz"));
        Assert.assertEquals(xml.xpath("//project/dependencies/dependency/artifactId/text()"), Lists.newArrayList("xyz-aaa"));
        Assert.assertEquals(xml.xpath("//project/dependencies/dependency/version/text()"), Lists.newArrayList("1.0"));
    }




    @Test
    public void install() throws IOException {
        final Act.Memento act = new Act.Memento(
                new Acts.Deps()
        );
        File tmpWorkDir = Files.createTempDir();
        Path jar = java.nio.file.Files.createTempFile("jar", "jar");

        Files.touch(jar.toFile());
        jar.toFile().deleteOnExit();
        tmpWorkDir.deleteOnExit();

        class Tmp implements Project {
            @Override
            public Path workDir() {
                return tmpWorkDir.toPath();
            }

            @Override
            public Path m2Home() {
                return tmpWorkDir.toPath();
            }

            @Override
            public Iterable<Dep> deps() {
                return Lists.newArrayList(
                        new Dep.Simple(jar.toFile(), "xyz.com.baz", "xyz-aaa", "1.0"));
            }
        }

        act.accept(new Tmp());

        final Path resolve = tmpWorkDir.toPath().resolve("repository/xyz/com/baz/xyz-aaa/1.0/");
        Assert.assertTrue(resolve.resolve("xyz-aaa-1.0.jar").toFile().exists());
        Assert.assertTrue(resolve.resolve("xyz-aaa-1.0.pom").toFile().exists());
    }
}
