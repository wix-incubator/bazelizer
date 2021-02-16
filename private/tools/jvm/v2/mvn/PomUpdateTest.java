package tools.jvm.v2.mvn;

import org.cactoos.io.InputOf;
import org.junit.Assert;
import org.junit.Test;


public class PomUpdateTest {

    @Test
    public void drops() {
        final String pomStr =
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                        "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                        "         xmlns:bz=\"https://github.com/wix-incubator/bazelizer\"\n" +
                        "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                        "\n" +
                        "\n" +
                        "    <modelVersion>4.0.0</modelVersion>\n" +
                        "    <groupId>com.mavenizer.examples-single.lib</groupId>\n" +
                        "    <artifactId>myapi-single</artifactId>\n" +
                        "    <version>1.0.0</version>\n" +
                        "\n" +
                        "\n" +
                        "    <dependencies >\n" +
                        "        <dependency>\n" +
                        "            <groupId>com.google.guava</groupId>\n" +
                        "            <artifactId>guava</artifactId>\n" +
                        "            <version>30.0-jre</version>\n" +
                        "        </dependency>\n" +
                        "    </dependencies>\n" +
                        "\n" +
                        "</project>";

        final String expStr =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:bz=\"https://github.com/wix-incubator/bazelizer\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                        "  <modelVersion>4.0.0</modelVersion>\n" +
                        "  <groupId>com.mavenizer.examples-single.lib</groupId>\n" +
                        "  <artifactId>myapi-single</artifactId>\n" +
                        "  <version>1.0.0</version>\n" +
                        "  <dependencies/>\n" +
                        "</project>\n";

        final Pom pom = new Pom.Std(new InputOf(pomStr)).update(
                new PomUpdate.PomDropDeps()
        );
        final String res = pom.asString();
        Assert.assertEquals(expStr, res);
    }

    @Test
    public void notRemoves() {
        final String pomStr =
                "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                        "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                        "         xmlns:bz=\"https://github.com/wix-incubator/bazelizer\"\n" +
                        "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                        "\n" +
                        "\n" +
                        "    <modelVersion>4.0.0</modelVersion>\n" +
                        "    <groupId>com.mavenizer.examples-single.lib</groupId>\n" +
                        "    <artifactId>myapi-single</artifactId>\n" +
                        "    <version>1.0.0</version>\n" +
                        "\n" +
                        "\n" +
                        "    <dependencies bz:drop=\"never\">\n" +
                        "        <dependency>\n" +
                        "            <groupId>com.google.guava</groupId>\n" +
                        "            <artifactId>guava</artifactId>\n" +
                        "            <version>30.0-jre</version>\n" +
                        "        </dependency>\n" +
                        "    </dependencies>\n" +
                        "\n" +
                        "</project>";

        final String expStr =
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
                        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:bz=\"https://github.com/wix-incubator/bazelizer\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
                        "  <modelVersion>4.0.0</modelVersion>\n" +
                        "  <groupId>com.mavenizer.examples-single.lib</groupId>\n" +
                        "  <artifactId>myapi-single</artifactId>\n" +
                        "  <version>1.0.0</version>\n" +
                        "  <dependencies bz:drop=\"never\">\n" +
                        "    <dependency>\n" +
                        "      <groupId>com.google.guava</groupId>\n" +
                        "      <artifactId>guava</artifactId>\n" +
                        "      <version>30.0-jre</version>\n" +
                        "    </dependency>\n" +
                        "  </dependencies>\n" +
                        "</project>\n";

        final Pom pom = new Pom.Std(new InputOf(pomStr)).update(
                new PomUpdate.PomDropDeps()
        );
        final String res = pom.asString();
        Assert.assertEquals(expStr, res);
    }

}
