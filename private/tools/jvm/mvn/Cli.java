package tools.jvm.mvn;


import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.cactoos.io.InputOf;
import org.cactoos.io.OutputTo;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class Cli {

//    static {
//        SLF4JConfigurer.setLogLevel(SLF4JConfigurer.ToolLogLevel.DEBUG);
//    }

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

//        static {
//            SLF4JConfigurer.setLogLevel(SLF4JConfigurer.ToolLogLevel.DEBUG);
//        }

        @CommandLine.Mixin
        public ArgsFactory argsFactory = new ArgsFactory();


        @CommandLine.Option(names = {"--def"}, description = "Rule specific output settings")
        public Path pomDeclarations;

        @CommandLine.Option(names = {"-s", "--settings"}, description = "External settings xml")
        public Path globalSettingsXml;

        @CommandLine.Option(names = {"-gm", "--global-manifest"},
                paramLabel = "PATH", description = "desired output for run manifest")
        public Path globalRepositoryManifest;

        @CommandLine.Option(names = {"-rs", "--mk-snapshot"},
                paramLabel = "PATH", description = "desired output for repo snapshot")
        public Path repositorySnapshot;

        @SneakyThrows
        @Override
        public void run() {
            final Maven maven = new Maven.BazelInvoker();
            final Args args = argsFactory.newArgs();
            args.tag(Args.FlagsKey.SETTINGS_XML, globalSettingsXml.toFile());

            final Project simple = Project.builder()
                    .args(args)
                    .build();

            new Act.Iterative(
                    new ActGlobalSettings(
                            new InputOf(globalSettingsXml),
                            new OutputTo(globalRepositoryManifest),
                            this.repositorySnapshot
                    ),
                    new ActAssemble(
                            new Builds.PomDefinitions(pomDeclarations),
                            new Act.Iterative(
                                    new Acts.InstallParentPOM(
                                            maven
                                    ),
                                    new Acts.ParentPOM(),
                                    new Acts.PomFile(),
                                    new Acts.MvnGoOffline(
                                            maven
                                    )
                            )
                    ),
                    new Acts.Outputs()
            ).accept(simple);
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

        @CommandLine.Option(names = {"--m2-repository"},
                paramLabel = "REPO", description = "the repository tar")
        public Path extRepository;

        @CommandLine.Option(names = {"--run-manifest"}, required = true,
                paramLabel = "REPO", description = "the repository tar")
        public Path runManifest;

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
                        return new OutputFile.TargetFolderFile(buildFile, declared);
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
                    .runManifest(new RunManifest(runManifest))
                    .build();

            new Act.Iterative(
                    new Acts.Repository(
                            extRepository
                    ),
                    new Acts.SettingsXml(),
                    new Acts.Deps(),
                    new Acts.InstallParentPOM(
                            maven
                    ),
                    new Acts.ParentPOM(),
                    new Acts.PomFile(),
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
            return new Deps.Manifest(deps).stream().collect(Collectors.toSet());
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
        log.info("***  time elapsed: {}s", Duration.between(from, to).getSeconds());
        System.exit(exitCode);
    }
}
