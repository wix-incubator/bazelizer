package tools.jvm.mvn;


import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class Cli {

    @SuppressWarnings("UnstableApiUsage")
    @CommandLine.Command(name = "repo2tar")
    public static class Snapshot implements Runnable, Project {

        @CommandLine.Option(names = { "-pt", "--pom" }, paramLabel = "POM", description = "the pom xml template file")
        public Path pomXmlTpl;

        @CommandLine.Option(names = { "-o", "--output" }, paramLabel = "PATH", description = "desired output for repo snapshot")
        public Path output;

        @Override
        public ByteSource pomXmlTpl() {
            return Files.asByteSource(pomXmlTpl.toFile());
        }

        @Override
        public Path workDir() {
            return pomXmlTpl.getParent();
        }

        @Override
        public Iterable<Output> getOutputs() {
            return ImmutableList.of(
                    new Project.TmpSrc(output)
            );
        }

        @Override
        public void run() {
            final Project constant = Project.memento(this);
            new Act.Iterative(
                    new Acts.PomMustache(),
                    new Acts.SettingsXml(),
                    new Acts.MvnBuild(false),
                    new Acts.MkRepoSnapshot(),
                    new Acts.Outputs()
            ).accept(constant);
        }
    }



    @CommandLine.Command(name = "build")
    public static class Build implements Runnable, Project {
        @CommandLine.Option(names = { "-pt", "--pom" }, paramLabel = "POM", description = "the pom xml template file")
        public Path pomXmlTpl;

        @CommandLine.Option(names = { "-r", "--repo" }, paramLabel = "REPO", description = "the repository tar")
        public Path repo;

        @CommandLine.Option(names = { "-dp", "--deps" }, paramLabel = "DEPS", description = "the deps manifest")
        public File deps;

        @CommandLine.Option(names = { "-s", "--srcs" }, paramLabel = "SRCS", description = "the srcs manifest")
        public File srcs;

        @CommandLine.Option(names = { "-a", "--args" }, paramLabel = "ARGS", description = "the maven cli args")
        public String args;

        @CommandLine.Option(names = { "-ai", "--groupId" }, defaultValue = "groupId",
                paramLabel = "ID", description = "the maven groupId")
        public String groupId;

        @CommandLine.Option(names = { "-gi", "--artifactId" }, defaultValue = "artifactId",
                paramLabel = "ID", description = "the maven artifactId")
        public String artifactId;

        @CommandLine.Option(names = { "-O", "--outputs" },
                description = "the output: desired file -> source file in <workspace>/target")
        public Map<String,String> outputs;

        @Override
        public String artifactId() {
            return artifactId;
        }

        @Override
        public String groupId() {
            return groupId;
        }

        @Override
        public Iterable<Dep> deps() {
            return PathsCollection.fromManifest(deps)
                    .stream().map(Dep.DigestCoords::new)
                    .collect(Collectors.toSet());
        }

        @Override
        public Path workDir() {
            final PathsCollection manifest = PathsCollection.fromManifest(srcs);
            return manifest.commonPrefix()
                    .orElseThrow(() -> new NoCommonPrefixForSrcsException("srcs " + manifest));
        }

        @Override
        public Iterable<Output> getOutputs() {
            return outputs.entrySet()
                    .stream()
                    .map(entry -> new Project.OutputPaths(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
        }

        @Override
        public Path repoImage() {
            return repo;
        }

        @Override
        public ByteSource pomXmlTpl() {
            return Files.asByteSource(pomXmlTpl.toFile());
        }

        @Override
        @lombok.SneakyThrows
        public void run() {
            final Project constant = Project.memento(this);
            new Act.Iterative(
                    new Acts.DefRepository(),
                    new Acts.Deps(),
                    new Acts.PomMustache(),
                    new Acts.SettingsXml(),
                    new Acts.MvnBuild(true),
                    new Acts.Outputs()
            ).accept(constant);
        }
    }



    @CommandLine.Command(subcommands = {
            Snapshot.class,
            Build.class
    })
    public static class Tool {
    }

    public static void main(String[] args) throws Exception {
        int exitCode = new CommandLine(new Tool()).execute(args);
        log.info("*************** Done. ***************");
        System.exit(exitCode);
    }

    static class NoCommonPrefixForSrcsException extends RuntimeException {
        public NoCommonPrefixForSrcsException(String message) {
            super(message);
        }
    }
}
