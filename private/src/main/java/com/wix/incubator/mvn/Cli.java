package com.wix.incubator.mvn;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

@CommandLine.Command(subcommands = {
        Cli.CmdRepository.class,
        Cli.CmdBuild.class,
})
public class Cli {

    static {
        System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");
    }

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Path.class, (JsonDeserializer<Path>) (json, typeOfT, context) -> {
                final String asString = json.getAsString();
                return Paths.get(asString);
            }).create();

    public static final MustacheFactory MUSTACHE =
            new DefaultMustacheFactory();

    public static void main(String[] args) {
        //noinspection InstantiationOfUtilityClass
        int exitCode = new CommandLine(new Cli()).execute(args);
        System.exit(exitCode);
    }

    @CommandLine.Command(name = "build")
    public static class CmdBuild extends Executable {
        @CommandLine.Option(names = {"--repository"}, paramLabel = "PATH")
        public Path repository;

        @CommandLine.Option(names = {"--deps"}, paramLabel = "PATH")
        public Path depsConfig;

        @CommandLine.Option(names = {"--pom"}, paramLabel = "PATH")
        public Path pomFile;

        @CommandLine.Option(names = {"-O"}, description = "declared bazel output -> relatice file path /target")
        public Map<String, String> outputs;

        @CommandLine.Option(names = {"--archiveOutput"}, paramLabel = "P",
                description = "write archived artifact from repo, except default jar")
        public Path archiveOutput;

        @CommandLine.Option(names = {"--jarOutput"}, paramLabel = "P",
                description = "write default jar")
        public Path jarOutput;

        @SuppressWarnings("Convert2MethodRef")
        public void invoke() throws Exception {
            Maven env = Maven.prepareEnv(
                    repository
            );

            List<Dep> deps = readLines(depsConfig).stream()
                    .map(jsonLine -> Dep.create(jsonLine))
                    .collect(Collectors.toList());

            MavenProject project = MavenProject.create(
                    pomFile
            );

            env.executeOffline(
                    project,
                    asList("clean", "install")
            );

            Files.copy(
                    project.jar(),
                    jarOutput
            );

            writeArchivedFolder(
                    env,
                    project,
                    archiveOutput
            );
        }

        private static void writeArchivedFolder(Maven mvn, MavenProject project, Path out) throws IOException {
            final Path installedFolder = project.folder();
            final Path repository = mvn.repository;
            final Collection<Path> files = FileUtils.listFiles(
                    repository.resolve(installedFolder).toFile(),
                    FileFilterUtils.and(
                            IO.REPOSITORY_FILES_FILTER,
                            FileFilterUtils.notFileFilter(FileFilterUtils.suffixFileFilter("pom"))
                    ),
                    FileFilterUtils.trueFileFilter()
            ).stream().map(File::toPath).collect(Collectors.toList());

            try (OutputStream output = Files.newOutputStream(out)) {
                IO.tar(files, output, aFile -> {
                    final Path filePath = aFile.toAbsolutePath();
                    return filePath.subpath(repository.getNameCount(), filePath.getNameCount());
                });
            }

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
            final Maven env = Maven.prepareEnv(
            );
            final List<MavenProject> projects = MavenProject.topologicSort(
                    readLines(configFile).stream()
                            .map(MavenProject::create)
                            .collect(Collectors.toList())
            );

            for (MavenProject mvnProject : projects) {
                env.execute(mvnProject, asList("dependency:go-offline", "clean", "install"));
            }

            IO.tarRepositoryRecursive(env, output);
        }
    }

    public static List<String> readLines(Path text) throws IOException {
        try (Stream<String> s = Files.lines(text)) {
            return s.map(p -> {
                String line = p.trim();
                if (line.startsWith("'") || line.startsWith("\"")) {
                    line = line.substring(1);
                }
                if (line.endsWith("'") || line.endsWith("\"")) {
                    line = line.substring(0, line.length() - 1);
                }
                return line;
            }).collect(Collectors.toList());
        }
    }

    public static abstract class Executable implements Callable<Void> {
        @Override
        public final Void call() throws Exception {
            invoke();
            return null;
        }

        public abstract void invoke() throws Exception;
    }


}