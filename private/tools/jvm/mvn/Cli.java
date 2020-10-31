package tools.jvm.mvn;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class Cli {


    static class ArgsFactory {

        @SuppressWarnings("unused")
        @CommandLine.Option(names = {"-a", "--args"}, paramLabel = "ARGS", description = "the maven cli args")
        private String argsLine;

        Args newArgs() {
            final Args args = new Args();
            if (this.argsLine != null) {
                String line = argsLine;
                if (line.startsWith("'") || line.startsWith("\"")) {
                    line = line.substring(1);
                }
                if (line.endsWith("'") || line.endsWith("\"")) {
                    line = line.substring(0, line.length() - 1);
                }
                args.parseCommandLine(line);
            }
            return args;
        }
    }

    @SuppressWarnings({"unused"})
    @CommandLine.Command(name = "build-repository")
    public static class MkRepository implements Runnable {

        @CommandLine.Mixin
        public ArgsFactory argsFactory = new ArgsFactory();

        @CommandLine.Option(names = {"-pt", "--pomFile"},
                paramLabel = "POM", description = "the pom xml template file")
        public Path runPom;

        @CommandLine.Option(names = {"-wi", "--writeImg"},
                paramLabel = "PATH", description = "desired output for repo snapshot")
        public Path writeImg;


        @CommandLine.Option(names = {"--def"}, description = "Rule specific output settings")
        public Path pomDeclarations;


        @CommandLine.Option(names = {"--local-cache"}, description = "M2 host's m2 cache")
        public Path hostMavenCache;

        @SneakyThrows
        @Override
        public void run() {

            final Maven maven = new Maven.BazelInvoker();
            final Args args = argsFactory.newArgs();

            final Project simple = Project.builder()
                    .args(args)
                    .build();

            simple.outputs().add(
                    new OutputFile.ProjectFor(
                            Archive.LocalRepositoryDir::new,
                            writeImg.toString()
                    )
            );

            new Act.Iterative(
                    new Acts.SettingsXml(
                            new Template.Mustache<>(),
                            new Repositories.BazelLinkedLocalM2(Paths.get("/Users/bohdans/.m2/repository")) // TODO
                    ),
                    new ActAssemble(
                            new Builds.DefPomIterable(pomDeclarations),
                            new Act.Iterative(
                                    new Acts.InstallParentPOM(
                                            maven
                                    ),
                                    new Acts.ParentPOM(),
                                    new Acts.POM(),
                                    new Acts.MvnGoOffline(
                                            maven
                                    )
                            )
                    ),
                    new Acts.Outputs()
            ).accept(simple);

            log.info("Consolidated repository archived: "
                    + FileUtils.byteCountToDisplaySize(writeImg.toFile().length()));
        }

        @SuppressWarnings("UnstableApiUsage")
        private ByteSource getPomXmlSrc() {
            return Files.asByteSource(runPom.toFile());
        }
    }


    @SuppressWarnings({"unused", "UnstableApiUsage", "RedundantSuppression"})
    @CommandLine.Command(name = "run")
    public static class Run implements Runnable {

        @CommandLine.Mixin
        public ArgsFactory argsFactory = new ArgsFactory();

        @CommandLine.Option(names = {"--pom"}, required = true,
                paramLabel = "POM", description = "the pom xml template file")
        public Path pom;

        @CommandLine.Option(names = {"--repo"}, required = true,
                paramLabel = "REPO", description = "the repository tar")
        public Path repository;

        @CommandLine.Option(names = {"--deps"}, paramLabel = "DEPS", description = "the deps manifest")
        public File deps;

        @CommandLine.Option(names = {"--srcs"}, paramLabel = "SRCS",
                required = true, description = "the srcs manifest")
        public File srcs;

        @CommandLine.Option(names = {"-O"}, description = "declared bazel output -> relatice file path /target")
        public Map<String, String> outputs = ImmutableMap.of();

        @CommandLine.Option(names = {"--parent-pom"}, paramLabel = "P", description = "parent pom path")
        public Path parentPom;

        @CommandLine.Option(names = {"-wid", "--write-artifact"}, paramLabel = "P",
                description = "write archived artifact from repo, except default jar")
        public Path writeInstalledArtifact;

        @CommandLine.Option(names = {"-wdj", "--write-jar"}, paramLabel = "P",
                description = "write default jar")
        public Path writeDefaultJar;

        @Override
        @lombok.SneakyThrows
        public void run() {

            final Path workDir = getWorkDir();
            final Path pom = Project.syntheticPomFile(workDir);
            final Args args = argsFactory.newArgs();
            final Maven.BazelInvoker maven = new Maven.BazelInvoker();

            final List<OutputFile> outputs = this.outputs.entrySet()
                    .stream()
                    .map(entry -> {
                        final String declared = entry.getKey();
                        final String buildFile = entry.getValue();
                        return new OutputFile.Simple(buildFile, declared);
                    })
                    .collect(Collectors.toList());

            final Project project = Project.builder()
                    .parentPom(parentPom)
                    .pom(pom)
                    .args(args)
                    .deps(getDeps())
                    .workDir(workDir)
                    .pomTemplate(Files.asByteSource(this.pom.toFile()))
                    .outputs(outputs)
                    .build();


            new Act.Iterative(
                    new Acts.Repository(
                            repository
                    ),
                    new Acts.SettingsXml(
                            new Template.Mustache<>()
                    ),
                    new Acts.Deps(),
                    new Acts.ParentPOM(),
                    new Acts.POM(),
                    new Acts.MvnBuild(
                            maven
                    ),
                    new Acts.AppendDefaultOutputs(
                            writeDefaultJar,
                            writeInstalledArtifact
                    ),
                    new Acts.Outputs()
            ).accept(project);
        }

        private Set<Dep> getDeps() {
            return new Deps.Manifest(deps)
                    .stream()
                    .collect(Collectors.toSet());
        }

        private Path getWorkDir() {
            return new Deps.Manifest(srcs).resolveCommonPrefix();
        }

    }

    @CommandLine.Command(subcommands = {
            MkRepository.class,
            Run.class
    })
    public static class Tool {
    }

    public static void main(String[] args)  {
        LocalDateTime from = LocalDateTime.now();
        int exitCode = new CommandLine(new Tool()).execute(args);
        log.info("*** {}", exitCode == 0 ? "DONE" : "FAIL");
        LocalDateTime to = LocalDateTime.now();
        log.info("***  Time elapsed: {}s", Duration.between(from, to).getSeconds());
        System.exit(exitCode);
    }
}
