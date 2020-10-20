package tools.jvm.mvn;


import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class Cli {


    static class ExtraFlags {

        @CommandLine.Option(names = {"-a", "--args"}, paramLabel = "ARGS", description = "the maven cli args")
        public String argsLine;

        Args newArgs() {
            final Args args = new Args();
            if (this.argsLine != null) {
                args.parseCommandLine(argsLine);
            }
            return args;
        }
    }

    @SuppressWarnings({"UnstableApiUsage", "unused"})
    @CommandLine.Command(name = "build-repository")
    public static class MkRepository implements Runnable {

        public static final String GROUP_ID = "io.bazelbuild";

        @CommandLine.Option(names = {"-pt", "--pomFile"}, required = false,
                paramLabel = "POM", description = "the pom xml template file")
        public Path runPom;

        @CommandLine.Option(names = {"-wi", "--writeImg"}, required = false,
                paramLabel = "PATH", description = "desired output for repo snapshot")
        public Path writeImg;

        @CommandLine.Option(names = {"--def"}, description = "Rule specific output settings")
        public Path pomDeclarations;

        @SneakyThrows
        @Override
        public void run() {
            final Maven maven = new Maven.BazelInvoker();

            final Project simple = Project.builder().build();

            simple.outputs().add(
                    new OutputFile.DeclaredTarDir(
                            simple.repository(),
                            writeImg.toString()
                    )
            );

            new Act.Iterative(
                    new Acts.SettingsXml(),
                    new Projects(
                            pomDeclarations,
                            new Act.Iterative(
                                    new Acts.ParentPOM(),
                                    new Acts.POM(),
                                    new Acts.MvnGoOffline(
                                            maven
                                    )
                            )
                    ),
                    new Acts.Outputs()
            ).accept(simple);
        }

        private ByteSource getPomXmlSrc() {
            return Files.asByteSource(runPom.toFile());
        }
    }

    @SuppressWarnings({"UnstableApiUsage", "unused"})
    @CommandLine.Command(name = "repository")
    public static class Repository implements Runnable {

        @CommandLine.Mixin
        public ExtraFlags extraFlags = new ExtraFlags();

        public static final String GROUP_ID = "io.bazelbuild";
        @CommandLine.Option(names = {"-pt", "--pomFile"}, paramLabel = "POM", description = "the pom xml template file")
        public Path pomFile;

        @CommandLine.Option(names = {"-wi", "--writeImg"}, paramLabel = "PATH", description = "desired output for repo snapshot")
        public Path writeImg;

        @CommandLine.Option(names = {"-ppf", "--parentPomFile"}, paramLabel = "P", description = "parent pom path")
        public Path parentPomFile;

        @CommandLine.Option(names = {"-ppi", "--parentPomImg"}, paramLabel = "P", description = "parent pom path")
        public Path parentPomImg;

        @SneakyThrows
        @Override
        public void run() {
            final Project project = Project.builder()
                    .pomTemplate(getPomXmlSrc())
                    .groupId(GROUP_ID)
                    .artifactId("id-" + RandomText.randomLetters(6))
                    .workDir(pomFile.getParent())
                    .outputs(Lists.newArrayList())
                    .pomParent(parentPomFile)
                    .args(extraFlags.newArgs())
                    .build();

            project.outputs().add(new OutputFile.DeclaredProc(
                    new Archive.TarDirectory(project.repository()),
                    writeImg.toString()
            ));

            new Act.Iterative(
                    new Acts.SettingsXml(),
                    new Acts.Repository(
                            parentPomImg
                    ),
                    new Acts.ParentPOM(),
                    new Acts.InstallParentPOM(),
                    new Acts.POM(),
                    new Acts.MvnGoOffline(
                            new Maven.BazelInvoker()
                    ).compile(true),
                    new Acts.Outputs()
            ).accept(project);
        }

        private ByteSource getPomXmlSrc() {
            return Files.asByteSource(pomFile.toFile());
        }
    }



    @SuppressWarnings({"unused", "UnstableApiUsage"})
    @CommandLine.Command(name = "build")
    public static class Build implements Runnable {

        @CommandLine.Mixin
        public ExtraFlags extraFlags = new ExtraFlags();

        @CommandLine.Option(names = {"-pt", "--pom"}, required = true,
                paramLabel = "POM", description = "the pom xml template file")
        public Path pomXmlTpl;

        @CommandLine.Option(names = {"-r", "--repo"}, required = true,
                paramLabel = "REPO", description = "the repository tar")
        public Path repoTar;

        @CommandLine.Option(names = {"-dp", "--deps"}, paramLabel = "DEPS", description = "the deps manifest")
        public File deps;

        @CommandLine.Option(names = {"-s", "--srcs"}, paramLabel = "SRCS",
                required = true,
                description = "the srcs manifest")
        public File srcs;

        @CommandLine.Option(names = {"-ai", "--groupId"}, defaultValue = "groupId",
                paramLabel = "ID", description = "the maven groupId")
        public String groupId;

        @CommandLine.Option(names = {"-gi", "--artifactId"}, defaultValue = "artifactId",
                paramLabel = "ID", description = "the maven artifactId")
        public String artifactId;

        @CommandLine.Option(names = {"-O", "--outputs"},
                description = "the output: desired file -> source file in <workspace>/target")
        public Map<String, String> outputs = ImmutableMap.of();

        @CommandLine.Option(names = {"--defOutFlag"}, description = "Rule specific output settings")
        public Map<String, String> defOutputFlags;

        @CommandLine.Option(names = {"-pr", "--parent"}, paramLabel = "P", description = "parent pom path")
        public Path parent;

        @Override
        @lombok.SneakyThrows
        public void run() {
            final Path workDir = getWorkDir();
            final Path pom = Project.syntheticPomFile(workDir);
            final Args args = extraFlags.newArgs();

            final Project project = Project.builder()
                    .artifactId(artifactId)
                    .groupId(groupId)
                    .pomParent(parent)
                    .pom(pom)
                    .args(args)
                    .deps(getDeps())
                    .workDir(workDir)
                    .pomTemplate(Files.asByteSource(pomXmlTpl.toFile()))
                    .outputs(outputs.entrySet()
                            .stream()
                            .map(entry -> {
                                final String declared = entry.getKey();
                                final String buildFile = entry.getValue();
                                return new OutputFile.Simple(buildFile, declared);
                            })
                            .collect(Collectors.toList()))
                    .build();

            new Act.Iterative(
                    new Acts.Repository(
                            repoTar
                    ),
                    new Acts.SettingsXml(),
                    new Acts.Deps(),
                    new Acts.ParentPOM(),
                    new Acts.POM(),
                    new Acts.MvnBuild(
                            new Maven.BazelInvoker()
                    ),
                    new Acts.ArtifactPredefOutputs(defOutputFlags),
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
            Build.class,
            Repository.class,
            MkRepository.class
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
