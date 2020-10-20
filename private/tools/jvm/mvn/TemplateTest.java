package tools.jvm.mvn;

import com.google.common.io.CharSource;
import lombok.SneakyThrows;
import org.cactoos.Scalar;
import org.cactoos.io.InputOf;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TemplateTest {


    @SuppressWarnings("UnstableApiUsage")
    static class PomXPath extends Template.Mustache implements Template {

        public PomXPath(String source, File pom) {
            super(CharSource.wrap(source).asByteSource(StandardCharsets.UTF_8),
                    valueOf(new Pom.XPath(new InputOf(pom))));
        }

        public PomXPath(String source, String pom) {
            super(CharSource.wrap(source).asByteSource(StandardCharsets.UTF_8),
                    valueOf(new Pom.XPath((new InputOf(pom)))));
        }

        @SneakyThrows
        private static Pom.Props valueOf(Scalar<Pom.Props> xmlIn) {
            return xmlIn.value();
        }
    }


    @Test
    public void xpath() throws IOException {
        String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project>\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.wixpress.search</groupId>\n" +
                "    <artifactId>searcher-components-cfg</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "    <packaging>container-plugin</packaging>\n" +
                "</project>";

        String s = new PomXPath(
                "{{groupId}}-{{artifactId}}", pom
        ).eval().asString();


        Assert.assertEquals("com.wixpress.search-searcher-components-cfg", s);
    }

    @Test
    public void xpathWithNs() throws IOException {
        String pom = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.wixpress.search</groupId>\n" +
                "    <artifactId>searcher-components-cfg</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "    <packaging>container-plugin</packaging>\n" +
                "\n" +
                "    <!-- TODO tmp pom, get rid of when implement inheritance in mvn_buildpack rule  -->\n" +
                "\n" +
                "    <properties>\n" +
                "        <vespa.version>6.330.51</vespa.version>\n" +
                "        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n" +
                "         <!--\n" +
                "            DO NOT UPGRADE IF RUN INSIDE BAZEL BUILDS\n" +
                "            @see: https://stackoverflow.com/questions/40346225/maven-resources-plugin-symbolic-link-handling\n" +
                "         -->\n" +
                "        <maven-resources-plugin.version>2.7</maven-resources-plugin.version>\n" +
                "        <findbugs.version>1.3.9</findbugs.version>\n" +
                "    </properties>" +
                "</project>";

        String s = new PomXPath(
                "{{groupId}}-{{artifactId}}", pom
        ).eval().asString();


        Assert.assertEquals("com.wixpress.search-searcher-components-cfg", s);
    }
}
