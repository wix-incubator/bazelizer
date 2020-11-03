package tools.jvm.mvn;

import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.jcabi.xml.XML;
import org.cactoos.io.InputOf;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;

@SuppressWarnings({"UnstableApiUsage", "DuplicatedCode"})
public class PomTest {

    static String getNs(String s) {
        final String v = " xmlns=\"http://maven.apache.org/POM/4.0.0\" %s" +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\"";
        return String.format(v, s);
    }

    static String getNs() {
        return getNs( "");
    }

    static String FLAGS = "    <!-- xembly:on -->\n";

    final File tempDir = Files.createTempDir();


    @Test
    public void defStruc() throws Exception {
        for (String ns : new String[]{"", "" + getNs()}) {

            String xmlStr = "<project" + ns + ">\n" + FLAGS +
                    "    <modelVersion>4.0.0</modelVersion>\n" +
                    "    <!--suppress MavenRedundantGroupId -->\n" +
                    "    <groupId>com.mavenizer.examples.api</groupId>\n" +
                    "    <artifactId>myapi</artifactId>\n" +
                    "    <version>1.0.0-SNAPSHOT</version>\n" +
                    "\n" +
                    "\n" +
                    "</project>";

            String expectedXmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                    "<project" + ns + ">\n" + FLAGS +
                    "    <modelVersion>4.0.0</modelVersion>\n" +
                    "    <!--suppress MavenRedundantGroupId -->\n" +
                    "    <groupId>com.mavenizer.examples.api</groupId>\n" +
                    "    <artifactId>myapi</artifactId>\n" +
                    "    <version>1.0.0-SNAPSHOT</version>\n" +
                    "    <dependencies/>\n" +
                    "</project>\n";

            final XML xml = eval(xmlStr);
            final String resXML = xml.toString();
            Assert.assertEquals(resXML, expectedXmlStr, resXML);
        }
    }

    @Test
    public void defParent() throws Exception {
        for (String ns : new String[]{"", "" + getNs()}) {

            String xmlStr = "<project" + ns + ">\n" + FLAGS +
                    "    <modelVersion>4.0.0</modelVersion>\n" +
                    "    <!--suppress MavenRedundantGroupId -->\n" +
                    "    <groupId>com.mavenizer.examples.api</groupId>\n" +
                    "    <artifactId>myapi</artifactId>\n" +
                    "    <version>1.0.0-SNAPSHOT</version>\n" +
                    "\n" +
                    "    <parent>\n" +
                    "        <groupId>com.mavenizer.examples.api</groupId>\n" +
                    "        <artifactId>myapi-parent</artifactId>\n" +
                    "        <version>1.0.0-SNAPSHOT</version>\n" +
                    "        <relativePath>PATHPATHPATH</relativePath>\n" +
                    "    </parent>" +
                    "    <dependencies/>\n" +
                    "</project>";

            String expectedXmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                    "<project" + ns + ">\n" + FLAGS +
                    "    <modelVersion>4.0.0</modelVersion>\n" +
                    "    <!--suppress MavenRedundantGroupId -->\n" +
                    "    <groupId>com.mavenizer.examples.api</groupId>\n" +
                    "    <artifactId>myapi</artifactId>\n" +
                    "    <version>1.0.0-SNAPSHOT</version>\n" +
                    "    <parent>\n" +
                    "        <groupId>com.mavenizer.examples.api</groupId>\n" +
                    "        <artifactId>myapi-parent</artifactId>\n" +
                    "        <version>1.0.0-SNAPSHOT</version>\n" +
                    "        <relativePath>../parent-pom.xml</relativePath>\n" +
                    "    </parent>\n" +
                    "    <dependencies/>\n" +
                    "</project>\n";

            final XML xml = eval(xmlStr);
            final String resXML = xml.toString();
            Assert.assertEquals(resXML, expectedXmlStr, resXML);
        }
    }


    @Test
    public void rmDeps() throws Exception {
        for (String ns : new String[]{"", "" + getNs()}) {
            String xmlStr = "<project" + ns + ">\n" + FLAGS +
                    "    <modelVersion>4.0.0</modelVersion>\n" +
                    "    <!--suppress MavenRedundantGroupId -->\n" +
                    "    <groupId>com.mavenizer.examples.api</groupId>\n" +
                    "    <artifactId>myapi</artifactId>\n" +
                    "    <version>1.0.0-SNAPSHOT</version>\n" +
                    "\n" +
                    "    <parent>\n" +
                    "        <groupId>com.mavenizer.examples.api</groupId>\n" +
                    "        <artifactId>myapi-parent</artifactId>\n" +
                    "        <version>1.0.0-SNAPSHOT</version>\n" +
                    "        <relativePath>PATHPATHPATH</relativePath>\n" +
                    "    </parent>" +
                    "    <dependencies>\n" +
                    "        <dependency>\n" +
                    "            <groupId>javax.xml.bind</groupId>\n" +
                    "            <artifactId>jaxb-api</artifactId>\n" +
                    "        </dependency>\n" +
                    "        <dependency>\n" +
                    "            <groupId>com.sun.xml.bind</groupId>\n" +
                    "            <artifactId>jaxb-core</artifactId>\n" +
                    "        </dependency>\n" +
                    "    </dependencies>\n" +
                    "</project>";

            String expectedXmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                    "<project" + ns + ">\n" + FLAGS +
                    "    <modelVersion>4.0.0</modelVersion>\n" +
                    "    <!--suppress MavenRedundantGroupId -->\n" +
                    "    <groupId>com.mavenizer.examples.api</groupId>\n" +
                    "    <artifactId>myapi</artifactId>\n" +
                    "    <version>1.0.0-SNAPSHOT</version>\n" +
                    "    <parent>\n" +
                    "        <groupId>com.mavenizer.examples.api</groupId>\n" +
                    "        <artifactId>myapi-parent</artifactId>\n" +
                    "        <version>1.0.0-SNAPSHOT</version>\n" +
                    "        <relativePath>../parent-pom.xml</relativePath>\n" +
                    "    </parent>\n" +
                    "    <dependencies/>\n" +
                    "</project>\n";

            final XML xml = eval(xmlStr);
            final String resXML = xml.toString();
            Assert.assertEquals(resXML, expectedXmlStr, resXML);
        }
    }


    @Test
    public void rmDepsWithExclude() throws Exception {
        String newNs = "xmlns:xe=\"http://www.w3.org/1999/xhtml\"";
        for (String ns : new String[]{" " + newNs, getNs(newNs + " ")}) {

            String xmlStr = "<project" + ns + ">\n" + FLAGS +
                    "    <modelVersion>4.0.0</modelVersion>\n" +
                    "    <!--suppress MavenRedundantGroupId -->\n" +
                    "    <groupId>com.mavenizer.examples.api</groupId>\n" +
                    "    <artifactId>myapi</artifactId>\n" +
                    "    <version>1.0.0-SNAPSHOT</version>\n" +
                    "\n" +
                    "    <parent>\n" +
                    "        <groupId>com.mavenizer.examples.api</groupId>\n" +
                    "        <artifactId>myapi-parent</artifactId>\n" +
                    "        <version>1.0.0-SNAPSHOT</version>\n" +
                    "        <relativePath>PATHPATHPATH</relativePath>\n" +
                    "    </parent>" +
                    "    <dependencies>\n" +
                    "        <dependency xe:remove=\"never\">\n" +
                    "            <groupId>javax.xml.bind</groupId>\n" +
                    "            <artifactId>jaxb-api</artifactId>\n" +
                    "        </dependency>\n" +
                    "        <dependency>\n" +
                    "            <groupId>com.sun.xml.bind</groupId>\n" +
                    "            <artifactId>jaxb-core</artifactId>\n" +
                    "        </dependency>\n" +
                    "    </dependencies>\n" +
                    "</project>";

            String expectedXmlStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                    "<project" + ns + ">\n" + FLAGS +
                    "    <modelVersion>4.0.0</modelVersion>\n" +
                    "    <!--suppress MavenRedundantGroupId -->\n" +
                    "    <groupId>com.mavenizer.examples.api</groupId>\n" +
                    "    <artifactId>myapi</artifactId>\n" +
                    "    <version>1.0.0-SNAPSHOT</version>\n" +
                    "    <parent>\n" +
                    "        <groupId>com.mavenizer.examples.api</groupId>\n" +
                    "        <artifactId>myapi-parent</artifactId>\n" +
                    "        <version>1.0.0-SNAPSHOT</version>\n" +
                    "        <relativePath>../parent-pom.xml</relativePath>\n" +
                    "    </parent>\n" +
                    "    <dependencies>\n" +
                    "        <dependency xe:remove=\"never\">\n" +
                    "            <groupId>javax.xml.bind</groupId>\n" +
                    "            <artifactId>jaxb-api</artifactId>\n" +
                    "        </dependency>\n" +
                    "    </dependencies>\n" +
                    "</project>\n";

            final XML xml = eval(xmlStr);
            final String resXML = xml.toString();
            Assert.assertEquals(resXML, expectedXmlStr, resXML);
        }
    }

    public XML eval(String xmlStr) throws Exception {
        final Project build = Project.builder()
                .pomTemplate(CharSource.wrap(xmlStr).asByteSource(StandardCharsets.UTF_8))
                .workDir(tempDir.toPath())
                .parentPom(tempDir.toPath().resolve("parent-pom.xml"))
                .build();

        return new Pom.PomXempled(
                new InputOf(xmlStr),
                build
        ).xml();
    }
}
