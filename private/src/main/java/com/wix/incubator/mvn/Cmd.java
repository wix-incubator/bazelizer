package com.wix.incubator.mvn;

import com.google.common.base.Preconditions;
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
        Cmd.CmdBuild.class,
        Cmd.Info.class,
})
public class Cmd {

    public static class ExecutionOpts {

        @CommandLine.Option(names = {"--deps-drop-all"},
                description = "Delete all dependencies that declared in pom file before tool execution")
        public boolean dropAllDepsFromPom;

        @CommandLine.Option(names = {"--deps-drop-exclude"}, paramLabel = "<coors>",
                description = "Dependencies that satisfy an expression won't be deleted. " +
                "Expected a pattern in format '<groupId>:<artifactId>'. Also accept wildcard expressions. " +
                        "Examples: 'com.google.*:*', '*:guava', 'com.google.guava:failureaccess' ")
        public List<String> dropDepsExcludes = Collections.emptyList();

        @CommandLine.Option(names = {"--mvn-active-profiles"}, paramLabel = "<p>",
                description = "maven active profiles")
        public List<String> mavenActiveProfiles = Collections.emptyList();

        @CommandLine.Option(names = {"--mvn-extra-args"}, paramLabel = "<p>",
                description = "Maven extra commands")
        public List<String> mavenArgs = Collections.emptyList();

        @CommandLine.Option(names = {"--mvn-override-artifact-id"},
                paramLabel = "<artifactId>",
                description = "Change artifact id for maven project")
        public String overrideArtifactId;

        public Project.ModelVisitor visitor() {
            validate();
            Project.ModelVisitor visitor = Project.ModelVisitor.NOP;
            if (dropAllDepsFromPom) {
                visitor = visitor.andThen(new Project.DropAllDepsModelVisitor()
                        .addIgnores(dropDepsExcludes));
            }
            if (overrideArtifactId != null) {
                visitor = visitor.andThen(new Project.ChangeArtifactId(overrideArtifactId));
            }
            return visitor;
        }

        public void validate() {
            if (!dropDepsExcludes.isEmpty())
                Preconditions.checkArgument(dropAllDepsFromPom,
                        "--deps-drop-exclude param must be specified " +
                                "only with enabled --deps-drop-all flag");
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

            final Project.Args args = Project.Args.builder()
                    .deps(deps)
                    .cmd(asList("clean", "install"))
                    .modelVisitor(executionOptions.visitor())
                    .profiles(executionOptions.mavenActiveProfiles)
                    .build();

            final List<Out> outputs = registeredOutputs();
            outputs.add(new Out.Jar(jarOutput));
            outputs.add(new Out.Installed(archiveOutput));

            env.executeOffline(
                    project,
                    args,
                    outputs
            );
        }

        private List<Out> registeredOutputs() {
            return this.outputs.entrySet().stream()
                    .map(e -> new Out.TargetFile(e.getValue(), Paths.get(e.getKey())))
                    .collect(Collectors.toList());
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
                    .cmd(asList("clean", "dependency:go-offline", "dependency:resolve-plugins", "dependency:resolve"))
                    .build();

            env.executeInOrder(
                    projects,
                    build
            );

            long size = env.tarRepositoryRecursive(
                    output
            );

            Console.printSeparator();
            Console.info("Build finished. Archived repository " + FileUtils.byteCountToDisplaySize(size));
        }
    }

    @CommandLine.Command(name = "info")
    public static class Info {
        @CommandLine.Mixin
        public ExecutionOpts ots;
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
