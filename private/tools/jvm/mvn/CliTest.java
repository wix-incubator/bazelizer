package tools.jvm.mvn;

import org.junit.Assert;
import org.junit.Test;
import picocli.CommandLine;

import java.nio.file.Paths;
import java.util.Collections;

public class CliTest {

    @Test
    public void parseRunCmd() {
        final Cli.Run command = new Cli.Run();
        new CommandLine(command).parseArgs(
                "-Oxxx=yyy",
                "--pom=/some/pom.xml",
                "--repo=/some/repo.tar",
                "--srcs=/some/manifest.xml");
        Assert.assertEquals(command.outputs, Collections.singletonMap("xxx", "yyy"));
    }

    @Test
    public void pathS() {
        System.out.println(SLF4JConfigurer.shortPath(
                Paths.get("/Users/bohdans/Projects/vespa-services/deviantart/da-searcher-service/src/main/java/com/wixpress/search/deviantart/searcher/services/pom.xml")
        ));
    }
}
