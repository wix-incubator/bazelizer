package tools.jvm.v2.mvn;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.jcabi.xml.XML;
import lombok.SneakyThrows;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.cactoos.io.InputOf;
import org.xembly.Directive;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Build implements Runnable {
    @CommandLine.Option(names = {"--pom"}, required = true,
            paramLabel = "POM", description = "the pom xml template file")
    public Path pomFile;

    @CommandLine.Option(names = {"--m2-repository"},
            paramLabel = "REPO", description = "the repository tar")
    public Path repositoryTar;

    @CommandLine.Option(names = {"--deps"}, paramLabel = "DEPS", description = "the deps manifest")
    public Path deps;

    @CommandLine.Option(names = {"-O"}, description = "declared bazel output -> relatice file path /target")
    public Map<String, String> outputs = ImmutableMap.of();

    @CommandLine.Option(names = {"-wid", "--write-artifact"}, paramLabel = "P",
            description = "write archived artifact from repo, except default jar")
    public Path writeInstalledArtifact;

    @CommandLine.Option(names = {"-wdj", "--write-jar"}, paramLabel = "P",
            description = "write default jar")
    public Path writeDefaultJar;

    @SneakyThrows
    @Override
    public void run() {
        final Iterable<Dep> deps = Dep.load(
                new Manifest(this.deps)
        );

        final Builds builds = new Builds(
                new InputOf(repositoryTar)
        );

        builds.installDeps(
                deps
        );

        final Pom pom = new Pom.Std(new InputOf(pomFile)).update(
                new PomUpdate.PomStruc(),
                new PomUpdate.PomDropDeps(),
                new PomUpdate.AppendDeps(deps)
        );

    }


    @SneakyThrows
    private File saveFile(Pom pom) {
        final Path abs = pomFile.getParent().resolve("pom." + Builds.LABEL + ".xml");
        if (Files.notExists(abs)) {
            Files.write(abs, pom.toString().getBytes());
        }
        return abs.toFile();
    }

    @SneakyThrows
    public void exec(Builds maven) {
        final Path pomFile = toFile().toPath();
        DefaultInvoker invoker = new DefaultInvoker();
        invoker.setMavenHome(Builds.MAVEN_TOOL);
        invoker.setWorkingDirectory(pomFile.getParent().toFile());

        final DefaultInvocationRequest request = maven.newRequest();
        request.setPomFile(pomFile.toFile());
        invoker.execute(request);
    }
}
