package tools.jvm.v2.mvn;

import org.cactoos.io.InputOf;
import org.cactoos.text.TextOf;
import org.junit.Assert;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

public class PomTest {

    @Test
    public void xmlns() {
        final Pom pom = new Pom.Std(new InputOf(new TextOf(
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xmlns:bz=\"https://github.com/wix-incubator/bazelizer\"\n" +
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                "\n" +
                "\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.mavenizer.examples-single.lib</groupId>\n" +
                "    <artifactId>myapi-single</artifactId>\n" +
                "    <version>1.5.2</version>\n" +
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
                "</project>"))
        );

        Assert.assertEquals("myapi-single", pom.artifactId());
        Assert.assertEquals("com.mavenizer.examples-single.lib", pom.groupId());
        Assert.assertEquals("1.5.2", pom.version());
    }


    @Test
    public void noXmlns() {
        final Pom pom = new Pom.Std(new InputOf(new TextOf(
                "<project >\n" +
                        "\n" +
                        "\n" +
                        "    <modelVersion>4.0.0</modelVersion>\n" +
                        "    <groupId>com.mavenizer.examples-single.lib</groupId>\n" +
                        "    <artifactId>myapi-single</artifactId>\n" +
                        "    <version>1.5.2</version>\n" +
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
                        "</project>"))
        );

        Assert.assertEquals("myapi-single", pom.artifactId());
        Assert.assertEquals("com.mavenizer.examples-single.lib", pom.groupId());
        Assert.assertEquals("1.5.2", pom.version());
    }


    @Test
    public void xmlnsParent1() {
        final Pom pom = new Pom.Std(new InputOf(new TextOf(
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                        "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                        "         xmlns:bz=\"https://github.com/wix-incubator/bazelizer\"\n" +
                        "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                        "\n" +
                        "\n" +
                        "    <modelVersion>4.0.0</modelVersion>\n" +
                        "    <artifactId>myapi-single-xyz-001</artifactId>\n" +
                        "    <version>1.0.1-SNAPSHOT</version>\n" +
                        "\n" +
                        "    <parent>\n" +
                        "        <groupId>com.mavenizer.xyz</groupId>\n" +
                        "        <artifactId>myapi-parent</artifactId>\n" +
                        "        <version>1.0.0-SNAPSHOT</version>\n" +
                        "        <relativePath>../mvn-lib-parent/pom.xml</relativePath>\n" +
                        "    </parent>" +
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
                        "</project>"))
        );

        Assert.assertEquals("myapi-single-xyz-001", pom.artifactId());
        Assert.assertEquals("com.mavenizer.xyz", pom.groupId());
        Assert.assertEquals("1.0.1-SNAPSHOT", pom.version());
    }


    @Test
    public void xmlnsParent2() {
        final Pom pom = new Pom.Std(new InputOf(new TextOf(
                "<project >\n" +
                        "\n" +
                        "\n" +
                        "    <modelVersion>4.0.0</modelVersion>\n" +
                        "    <artifactId>myapi-single-xyz-001</artifactId>\n" +
                        "    <version>1.0.1-SNAPSHOT</version>\n" +
                        "\n" +
                        "    <parent>\n" +
                        "        <groupId>com.mavenizer.xyz</groupId>\n" +
                        "        <artifactId>myapi-parent</artifactId>\n" +
                        "        <version>1.0.0-SNAPSHOT</version>\n" +
                        "        <relativePath>../mvn-lib-parent/pom.xml</relativePath>\n" +
                        "    </parent>" +
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
                        "</project>"))
        );

        Assert.assertEquals("myapi-single-xyz-001", pom.artifactId());
        Assert.assertEquals("com.mavenizer.xyz", pom.groupId());
        Assert.assertEquals("1.0.1-SNAPSHOT", pom.version());
        Assert.assertEquals(Paths.get("com/mavenizer/xyz/myapi-single-xyz-001/1.0.1-SNAPSHOT"), pom.folder());
    }


    @Test
    public void xml() {
        final Pom pom = new Pom.Std(new InputOf(new TextOf(
                "<project >\n" +
                        "\n" +
                        "\n" +
                        "    <modelVersion>4.0.0</modelVersion>\n" +
                        "    <groupId>com.mavenizer.examples-single.lib</groupId>\n" +
                        "    <artifactId>myapi-single</artifactId>\n" +
                        "    <version>1.5.2</version>\n" +
                        "\n" +
                        "\n" +
                        "    <dependencies>\n" +
                        "        <dependency>\n" +
                        "            <groupId>com.yahoo.vespa</groupId>\n" +
                        "            <artifactId>container</artifactId>\n" +
                        "        </dependency>\n" +
                        "        <dependency>\n" +
                        "            <groupId>com.google.guava</groupId>\n" +
                        "            <artifactId>guava</artifactId>\n" +
                        "        </dependency>\n" +
                        "        <dependency>\n" +
                        "            <groupId>com.wixpress.search.srs</groupId>\n" +
                        "            <artifactId>srs-json-filter</artifactId>\n" +
                        "            <version>1.0.0-SNAPSHOT</version>\n" +
                        "        </dependency>" +
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
                        "</project>"))
        );

        Pom newPom = pom.update(
                new PomUpdate.BuildUpdates(Collections.singletonList(
                        Dep.hashed(Paths.get("/tmp/file/is/here.jar"))
                ))
        );

        final String pomXml = newPom.asString();
        final String expect = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<project>\n" +
                "  <modelVersion>4.0.0</modelVersion>\n" +
                "  <groupId>com.mavenizer.examples-single.lib</groupId>\n" +
                "  <artifactId>myapi-single</artifactId>\n" +
                "  <version>1.5.2</version>\n" +
                "  <dependencies>\n" +
                "    <dependency>\n" +
                "      <!-- by: /tmp/file/is/here.jar -->\n" +
                "      <groupId>io.bazelbuild.d074f939686d061d850088527fe1fc0e</groupId>\n" +
                "      <artifactId>here</artifactId>\n" +
                "      <version>rev-d074f93</version>\n" +
                "      <scope>compile</scope>\n" +
                "    </dependency>\n" +
                "  </dependencies>\n" +
                "  <build>\n" +
                "    <plugins>\n" +
                "      <plugin>\n" +
                "        <groupId>org.apache.maven.plugins</groupId>\n" +
                "        <artifactId>maven-compiler-plugin</artifactId>\n" +
                "        <configuration>\n" +
                "          <source>1.6</source>\n" +
                "          <target>1.6</target>\n" +
                "        </configuration>\n" +
                "      </plugin>\n" +
                "    </plugins>\n" +
                "  </build>\n" +
                "</project>" +
                "\n";
        Assert.assertEquals(expect, pomXml);
    }
}
