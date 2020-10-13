package tools.jvm.mvn;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class Cli {

    @SuppressWarnings("unused")
    static class LoggingMixin {

        SLF4JConfigurer.ToolLogLevel logLevel;

        /**
         * Sets the specified verbosity on the LoggingMixin of the top-level command.
         *
         * @param logLevel the new verbosity value
         */
        @CommandLine.Option(names = "--syslog", defaultValue = "OFF", fallbackValue = "INFO",
                description = {
                        "When specified without arguments, start sending syslog messages at INFO level.",
                        "If absent, no messages are sent to syslog.",
                        "Optionally specify a severity value. Valid values: ${COMPLETION-CANDIDATES}."})
        public void setLogLevel(SLF4JConfigurer.ToolLogLevel logLevel) {
            SLF4JConfigurer.logLevel(logLevel);
            this.logLevel = logLevel;
        }
    }


    @SuppressWarnings({"UnstableApiUsage", "unused"})
    @CommandLine.Command(name = "repo2tar")
    public static class Snapshot implements Runnable {
        @CommandLine.Mixin
        private LoggingMixin mixin;

        @CommandLine.Option(names = {"-pt", "--pom"}, paramLabel = "POM", description = "the pom xml template file")
        public Path pomXmlTpl;

        @CommandLine.Option(names = {"-o", "--output"}, paramLabel = "PATH", description = "desired output for repo snapshot")
        public Path output;

        @CommandLine.Option(names = {"-pr", "--parent"}, paramLabel = "P", description = "parent pom path")
        public Path parent;

        @Override
        public void run() {
            final Project project = Project.builder()
                    .pomXmlSrc(Files.asByteSource(pomXmlTpl.toFile()))
                    .workDir(pomXmlTpl.getParent())
                    .outputs(ImmutableList.of(new Output.TmpSrc(output)))
                    .pomParent(parent)
                    .build();

            new Act.Iterative(
                    new Acts.SettingsXml(),
                    new Acts.DefineParentPom(),
                    new Acts.InstallParentPOM(),
                    new Acts.POM(),
                    new Acts.MvnGoOffline(),
                    new Acts.RepositoryArchiver(),
                    new Acts.Outputs()
            ).accept(project);
        }
    }


    @SuppressWarnings({"unused", "UnstableApiUsage"})
    @CommandLine.Command(name = "build")
    public static class Build implements Runnable {
        @CommandLine.Mixin
        private LoggingMixin mixin;

        @CommandLine.Option(names = {"-pt", "--pom"}, required = true,
                paramLabel = "POM", description = "the pom xml template file")
        public Path pomXmlTpl;

        @CommandLine.Option(names = {"-r", "--repo"}, required = true,
                paramLabel = "REPO", description = "the repository tar")
        public Path repo;

        @CommandLine.Option(names = {"-dp", "--deps"}, paramLabel = "DEPS", description = "the deps manifest")
        public File deps;

        @CommandLine.Option(names = {"-s", "--srcs"}, paramLabel = "SRCS",
                required = true,
                description = "the srcs manifest")
        public File srcs;

        @CommandLine.Option(names = {"-a", "--args"}, paramLabel = "ARGS", description = "the maven cli args")
        public String args;

        @CommandLine.Option(names = {"-ai", "--groupId"}, defaultValue = "groupId",
                paramLabel = "ID", description = "the maven groupId")
        public String groupId;

        @CommandLine.Option(names = {"-gi", "--artifactId"}, defaultValue = "artifactId",
                paramLabel = "ID", description = "the maven artifactId")
        public String artifactId;

        @CommandLine.Option(names = {"-O", "--outputs"},
                description = "the output: desired file -> source file in <workspace>/target")
        public Map<String, String> outputs = ImmutableMap.of();

        @CommandLine.Option(names = {"-pr", "--parent"}, paramLabel = "P", description = "parent pom path")
        public Path parent;

        private Path getWorkDir() {
            return PathsCollection.fromManifest(srcs).commonPrefix();
        }

        private Path getPomFileDest(Path workDir) {
            return workDir.resolve(String.format("%s_%s.xml",
                    RandomText.randomStr("pom_", 14),
                    Long.toHexString(System.currentTimeMillis())));
        }

        @Override
        @lombok.SneakyThrows
        public void run() {
            final Path workDir = getWorkDir();
            final Path pom = getPomFileDest(workDir);

            final Project project = Project.builder()
                    .artifactId(artifactId)
                    .groupId(groupId)
                    .pomParent(parent)
                    .pom(pom)
                    .deps(PathsCollection.fromManifest(deps).stream().map(Dep.DigestCoords::new).collect(Collectors.toSet()))
                    .workDir(workDir)
                    .pomXmlSrc(Files.asByteSource(pomXmlTpl.toFile()))
                    .outputs(outputs.entrySet()
                            .stream()
                            .map(entry -> {
                                final String declared = entry.getKey();
                                final String buildFile = entry.getValue();
                                return new Output.Paths(buildFile, declared, pom.toFile());
                            })
                            .collect(Collectors.toList()))
                    .baseImage(repo)
                    .build();

            new Act.Iterative(
                    new Acts.DefRepository(),
                    new Acts.Version(),
                    new Acts.SettingsXml(),
                    new Acts.Deps(),
                    new Acts.DefineParentPom(),
                    new Acts.POM(),
                    new Acts.MvnBuildOffline(),
                    new Acts.Outputs()
            ).accept(project);
        }
    }


    @CommandLine.Command(subcommands = {
            Snapshot.class,
            Build.class
    })
    public static class Tool {
    }

    public static void main(String[] args)  {
        log.info("*************** Start ***************");
        LocalDateTime from = LocalDateTime.now();
        int exitCode = new CommandLine(new Tool()).execute(args);
        log.info("*************** {} ***************", exitCode == 0 ? "Done" : "Fail");
        LocalDateTime to = LocalDateTime.now();
        log.info("*****  Time elapsed: {}", Duration.between(from, to));
        System.exit(exitCode);
    }
}
