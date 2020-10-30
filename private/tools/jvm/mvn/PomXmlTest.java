package tools.jvm.mvn;

import com.google.common.collect.Lists;
import com.google.common.io.CharSource;
import org.cactoos.text.TextOf;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class PomXmlTest {
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
                "            <dependency xhtml:class=\"freeze\">\n" +
                "                <groupId>javax.xml.bind</groupId>\n" +
                "                <artifactId>jaxb-api</artifactId>\n" +
                "                <version>2.3.0</version>\n" +
                "            </dependency>\n" +
                "            <dependency>\n" +
                "                <groupId>com.sun.xml.bind</groupId>\n" +
                "                <artifactId>jaxb-core</artifactId>\n" +
                "                <version>2.3.0</version>\n" +
                "            </dependency>\n" +
                "            <dependency>\n" +
                "                <groupId>com.sun.xml.bind</groupId>\n" +
                "                <artifactId>jaxb-impl</artifactId>\n" +
                "                <version>2.3.0</version>\n" +
                "            </dependency>\n" +
                "            <dependency>\n" +
                "                <groupId>org.jvnet.jaxb2_commons</groupId>\n" +
                "                <artifactId>jaxb2-basics-runtime</artifactId>\n" +
                "                <version>1.11.1</version>\n" +
                "            </dependency>\n" +
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
                "        <relativePath>/foo/bar.xml</relativePath>\n" +
                "    </parent>\n" +
                "    <packaging>pom</packaging>\n" +
                "        <dependencies>\n" +
                "            \n" +
                "            \n" +
                "            \n" +
                "            \n" +
                "        <dependency>\n" +
                "            <!--source-of: /x/y/z -->\n" +
                "            <groupId>xxx</groupId>\n" +
                "            <artifactId>yyy</artifactId>\n" +
                "            <version>zzz</version>\n" +
                "            <scope>compile</scope>\n" +
                "        </dependency>\n" +
                "    </dependencies>\n" +
                "</project>\n";


        String resXML = new PomXml.Xemblerd(
                Project.builder()
                        .pomTemplate(CharSource.wrap(xml).asByteSource(StandardCharsets.UTF_8))
                        .deps(Lists.newArrayList(
                                new Dep.Simple(new File("/x/y/z"), "xxx", "yyy", "zzz")
                        ))
                        .parentPom(Paths.get("/foo/bar.xml"))
                        .build()
        ).apply(new TextOf(xml)).asString();

        System.out.println(resXML);
        assertEquals(expected, resXML);
    }

}
