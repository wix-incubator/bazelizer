package com.wix.incubator.mvn;

import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static com.wix.incubator.mvn.IOSupport.readLines;
import static java.util.Arrays.asList;

@CommandLine.Command(subcommands = {
        Cmd.CmdRepository.class,
        Cmd.CmdBuild.class
})
public class Cmd {

    public static class ExecutionOpts {

        @CommandLine.Option(names = {"--deps-drop-all"},
                description = "Delete all dependencies that declared in pom file before tool execution")
        public boolean dropAllDepsFromPom;

        @CommandLine.Option(names = {"--deps-drop-exclude"}, paramLabel = "<coors>", description = "Rules for deps drop exclusion, " +
                "rxpected format is '<groupId>:<artifactId>'. Examples: 'com.google.*:*', '*:guava', ect. ")
        public List<String> dropDepsExcludes = Collections.emptyList();

        @CommandLine.Option(names = {"--mvn-active-profiles"}, paramLabel = "<p>", description = "maven active profiles")
        public List<String> mavenActiveProfiles = Collections.emptyList();

        public Project.ModelVisitor visitor() {
            if (dropAllDepsFromPom) {
                return new Project.DropAllDepsModelVisitor()
                        .addIgnores(dropDepsExcludes);
            }
            return d -> {
            };
        }
    }


    @CommandLine.Command(name = "build")
    public static class CmdBuild extends Executable {

        @CommandLine.Mixin
        public ExecutionOpts executionOptions;

        @CommandLine.Option(names = {"--repository"}, paramLabel = "PATH")
        public Path repository;

        @CommandLine.Option(names = {"--deps"}, paramLabel = "PATH")
        public Path depsConfig;

        @CommandLine.Option(names = {"--pom"}, paramLabel = "PATH")
        public Path pomFile;

        @CommandLine.Option(names = {"-O"}, description = "declared bazel output -> relatvce file path /target")
        public Map<String, String> outputs = Collections.emptyMap();

        @CommandLine.Option(names = {"--archiveOutput"}, paramLabel = "P",
                description = "write archived artifact from repo, except default jar")
        public Path archiveOutput;

        @CommandLine.Option(names = {"--jarOutput"}, paramLabel = "P",
                description = "write default jar")
        public Path jarOutput;


        @SuppressWarnings("Convert2MethodRef")
        public void invoke() throws Exception {
            final Maven env = Maven.prepareEnvFromArchive(
                    repository
            );
            final Project project = Project.createProject(
                    pomFile
            );

            final List<Dep> deps = readLines(depsConfig).stream()
                    .map(jsonLine -> Dep.fromJson(jsonLine))
                    .collect(Collectors.toList());

            final Project.Args build = Project.Args.builder()
                    .deps(deps)
                    .cmd(asList("clean", "install"))
                    .modelVisitor(executionOptions.visitor())
                    .profiles(executionOptions.mavenActiveProfiles)
                    .build();

            env.executeOffline(
                    project,
                    build
            );

            final List<Project.Output> outputs = this.outputs.entrySet().stream()
                    .map(e -> new Project.Output(e.getValue(), Paths.get(e.getKey())))
                    .collect(Collectors.toList());

            project.save(
                    env,
                    jarOutput,
                    archiveOutput,
                    outputs
            );

        }

    }

    @CommandLine.Command(name = "build-repository")
    public static class CmdRepository extends Executable {

        @CommandLine.Option(names = {"--settingsXml"}, paramLabel = "PATH")
        public Path settingsXml;

        @CommandLine.Option(names = {"--config"}, paramLabel = "PATH")
        public Path configFile;

        @CommandLine.Option(names = {"--output"}, paramLabel = "PATH")
        public Path output;

        @Override
        public void invoke() throws Exception {
            final Maven env = Maven.prepareEnv(MvnRepository.fromFile(settingsXml));

            final List<Project> projects = readLines(configFile).stream()
                    .map(Project::createProject)
                    .collect(Collectors.toList());

            final Project.Args build = Project.Args.builder()
                    .cmd(asList("clean", "dependency:go-offline", "install"))
                    .build();

            env.executeInOrder(
                    projects,
                    build
            );

            long size = IOSupport.tarRepositoryRecursive(
                    env,
                    output
            );

            Console.printSeparator();
            Console.info("Build finished. Archived repository " + FileUtils.byteCountToDisplaySize(size));
        }
    }

    private static abstract class Executable implements Callable<Void> {
        @Override
        public final Void call() throws Exception {
            invoke();
            return null;
        }

        public abstract void invoke() throws Exception;
    }

}
