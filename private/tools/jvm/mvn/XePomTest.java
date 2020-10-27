package tools.jvm.mvn;

import org.cactoos.Text;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class XePomTest {

    @Test
    public void dirs() throws IOException {
        final Text eval = new Template.Xe(
                new Pom.StringOf(
                        "<project>\n" +
                                "    <modelVersion>4.0.0</modelVersion>\n" +
                                "    <groupId>com.mavenizer.examples.api</groupId>\n" +
                                "    <artifactId>myapi-parent</artifactId>\n" +
                                "    <version>1.0.0-SNAPSHOT</version>\n" +
                                "</project>"
                ),
                new XePom.DependenciesTag()
        ).eval();

        String exp = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                "<project>\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.mavenizer.examples.api</groupId>\n" +
                "    <artifactId>myapi-parent</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "<dependencies/>\n" +
                "</project>\n";


        Assert.assertEquals(exp, eval.asString());
    }


    @Test
    public void drop() throws IOException {
        final Pom pom = new Pom.StringOf(
                "<project xmlns:xe=\"http://www.w3.org/1999/xhtml\">\n" +
                        "    <dependencies>\n" +
                        "        <dependency>\n" +
                        "            <groupId>com.google.guava</groupId>\n" +
                        "            <artifactId>guava</artifactId>\n" +
                        "            <version>30.0-jre</version>\n" +
                        "        </dependency>\n" +
                        "        <dependency>\n" +
                        "            <groupId>javax.xml.bind</groupId>\n" +
                        "            <artifactId>jaxb-api</artifactId>\n" +
                        "        </dependency>\n" +
                        "        <dependency>\n" +
                        "            <groupId>com.sun.xml.bind</groupId>\n" +
                        "            <artifactId>jaxb-core</artifactId>\n" +
                        "        </dependency>\n" +
                        "        <dependency>\n" +
                        "            <groupId>com.sun.xml.bind</groupId>\n" +
                        "            <artifactId>jaxb-impl</artifactId>\n" +
                        "        </dependency>\n" +
                        "        <dependency>\n" +
                        "            <groupId>org.jvnet.jaxb2_commons</groupId>\n" +
                        "            <artifactId>jaxb2-basics-runtime</artifactId>\n" +
                        "        </dependency>\n" +
                        "        <dependency xe:keep=\"true\">\n" +
                        "            <groupId>junit</groupId>\n" +
                        "            <artifactId>junit</artifactId>\n" +
                        "            <version>4.13.1</version>\n" +
                        "            <scope>test</scope>\n" +
                        "        </dependency>\n" +
                        "    </dependencies>\n" +
                        "</project>");


        final Text eval = new Template.Xe(
                pom,
                new XePom.RemoveTags()
        ).eval();

        String exp =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                        "<project xmlns:xe=\"http://www.w3.org/1999/xhtml\">\n" +
                        "<dependencies>\n" +
                        "        <dependency xe:class=\"never\">\n" +
                        "            <groupId>junit</groupId>\n" +
                        "            <artifactId>junit</artifactId>\n" +
                        "            <version>4.13.1</version>\n" +
                        "            <scope>test</scope>\n" +
                        "        </dependency>\n" +
                        "</dependencies>\n" +
                        "</project>\n";

        final String string = eval.asString();
        Assert.assertEquals(exp, string);
    }
}
