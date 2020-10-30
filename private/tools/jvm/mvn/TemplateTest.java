package tools.jvm.mvn;

import com.google.common.collect.Lists;
import com.google.common.io.CharSource;
import org.cactoos.text.TextOf;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;


public class TemplateTest {

    @SuppressWarnings("UnstableApiUsage")
    @Test
    public void xe() throws IOException {

        String xml = "<project xmlns:xhtml=\"http://www.w3.org/1999/xhtml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.mavenizer.examples.api</groupId>\n" +
                "    <artifactId>myapi-parent</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "    <parent>\n" +
                "        <groupId>com.mavenizer.examples.api</groupId>\n" +
                "        <artifactId>myapi-parent</artifactId>\n" +
                "        <version>1.0.0-SNAPSHOT</version>\n" +
                "        <relativePath>{{ relative_path }}</relativePath>\n" +
                "    </parent>\n" +
                "    <packaging>pom</packaging>\n" +
                "        <dependencies>\n" +
                "        </dependencies>\n" +
                "</project>";

        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<project xmlns:xhtml=\"http://www.w3.org/1999/xhtml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.mavenizer.examples.api</groupId>\n" +
                "    <artifactId>myapi-parent</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "    <parent>\n" +
                "        <groupId>com.mavenizer.examples.api</groupId>\n" +
                "        <artifactId>myapi-parent</artifactId>\n" +
                "        <version>1.0.0-SNAPSHOT</version>\n" +
                "        <relativePath>../bar.xml</relativePath>\n" +
                "    </parent>\n" +
                "    <packaging>pom</packaging>\n" +
                "        <dependencies>\n" +
                "        <dependency>\n" +
                "            <!--source-of: /x/y/z -->\n" +
                "            <groupId>xxx</groupId>\n" +
                "            <artifactId>yyy</artifactId>\n" +
                "            <version>zzz</version>\n" +
                "            <scope>compile</scope>\n" +
                "        </dependency>\n" +
                "    </dependencies>\n" +
                "</project>\n";


        final Project.ProjectView projectView = Project.builder()
                .workDir(Paths.get("/tmp"))
                .pomTemplate(CharSource.wrap(xml).asByteSource(StandardCharsets.UTF_8))
                .deps(Lists.newArrayList(
                        new Dep.Simple(new File("/x/y/z"), "xxx", "yyy", "zzz")
                ))
                .parentPom(Paths.get("/tmp/bar.xml"))
                .build().toView();

        String resXML = new Template.Xembled(
                new TextOf(xml),
                projectView
        ).eval().asString();

        System.out.println(resXML);
        Assert.assertEquals(expected, resXML);
    }
}
