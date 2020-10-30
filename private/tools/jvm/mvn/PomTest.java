package tools.jvm.mvn;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class PomTest {

    @Test
    public void xe()  {

        String xml = "<project xmlns:xhtml=\"http://www.w3.org/1999/xhtml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.mavenizer.examples.api</groupId>\n" +
                "    <artifactId>myapi-parent</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
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
                "    <packaging>pom</packaging>\n" +
                "        <dependencies>\n" +
                "            \n" +
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
                "</project>\n";


        Pom pom = new Pom.StringOf(xml).transform(
                new XeSource.All(
                        new XeSource.HasDependenciesTag(),
                        new XeSource.RemoveDependencyTags()
                )
        );

        String resXML = pom.xml().toString();
        Assert.assertEquals(expected, resXML);
    }

    @Test
    public void xeDeps() {
        String xml = "<project xmlns:xhtml=\"http://www.w3.org/1999/xhtml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.mavenizer.examples.api</groupId>\n" +
                "    <artifactId>myapi-parent</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "    <packaging>pom</packaging>\n" +
                "</project>";

        Pom pom = new Pom.StringOf(xml).transform(
                new XeSource.All(
                        new XeSource.AddDeps(
                                Lists.newArrayList(
                                        new Dep.Simple(new File("/foo/bar/1"), "xx1", "yy_1", "1.0"),
                                        new Dep.Simple(new File("/foo/bar/2"), "xx1", "yy_2", "1.0")
                                )
                        )
                )
        );

        String resXML = pom.toPrettyXml();

        String expected = "<project xmlns:xhtml=\"http://www.w3.org/1999/xhtml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.mavenizer.examples.api</groupId>\n" +
                "    <artifactId>myapi-parent</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "    <packaging>pom</packaging>\n" +
                "    <dependencies>\n" +
                "      <!--by: file:///foo/bar/1-->\n" +
                "      <groupId>xx1</groupId>\n" +
                "      <artifactId>yy_1</artifactId>\n" +
                "      <version>1.0</version>\n" +
                "       </dependency>\n" +
                "    </dependencies>\n" +
                "</project>\n";

        System.out.println(resXML);
        Assert.assertEquals(expected, resXML);
    }


    @Test
    public void rmAndAdd() {
        String xml = "<project xmlns:xhtml=\"http://www.w3.org/1999/xhtml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.mavenizer.examples.api</groupId>\n" +
                "    <artifactId>myapi-parent</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "    <packaging>pom</packaging>\n" +
                "        <dependencies>\n" +
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

        Pom pom = new Pom.StringOf(xml).transform(
                new XeSource.RemoveDependencyTags(),
                new XeSource.AddDeps(
                        Lists.newArrayList(
                                new Dep.Simple(new File("/foo/bar/1"), "xx1", "yy_1", "1.0"),
                                new Dep.Simple(new File("/foo/bar/2"), "xx1", "yy_2", "1.0")
                        )
                )
        );

        String resXML = pom.toPrettyXml();

        String expected = "<project xmlns:xhtml=\"http://www.w3.org/1999/xhtml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <modelVersion>4.0.0</modelVersion>\n" +
                "    <groupId>com.mavenizer.examples.api</groupId>\n" +
                "    <artifactId>myapi-parent</artifactId>\n" +
                "    <version>1.0.0-SNAPSHOT</version>\n" +
                "    <packaging>pom</packaging>\n" +
                "    <dependencies>\n" +
                "      <!--by: file:///foo/bar/1-->\n" +
                "      <groupId>xx1</groupId>\n" +
                "      <artifactId>yy_1</artifactId>\n" +
                "      <version>1.0</version>\n" +
                "       </dependency>\n" +
                "    </dependencies>\n" +
                "</project>\n";

        System.out.println(resXML);
        Assert.assertEquals(expected, resXML);
    }
}
