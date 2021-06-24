package com.wix.incubator.mvn;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

        @CommandLine.Option(names = {"--drop-deps-all"})
        public boolean dropAllDepsFromPom;

        @CommandLine.Option(names = {"--drop-deps-ignore"}, paramLabel = "<coors>", description = "rules for deps filter")
        public List<String> dropDepsExcludes = Collections.emptyList();

        @CommandLine.Option(names = {"--mvn-active-profile"}, paramLabel = "<p>", description = "maven active profiles")
        public List<String> mavenActiveProfiles = Collections.emptyList();

        @SuppressWarnings("Convert2MethodRef")
        public void invoke() throws Exception {
            final Maven env = Maven.prepareEnvFromArchive(
                    repository
            );
            final Maven.Project project = Maven.createProject(
                    pomFile
            );

            List<Maven.DepsFilter> filters = new ArrayList<>();
            if (dropAllDepsFromPom) {
                filters.add(Maven.DepsFilter.falseFilter());
                for (String exclude : dropDepsExcludes) {
                    filters.add(Maven.DepsFilter.coords(exclude));
                }
            }

            final List<Dep> deps = readLines(depsConfig).stream()
                    .map(jsonLine -> Dep.create(jsonLine))
                    .collect(Collectors.toList());

            final Maven.DepsFilter filter = filters.stream()
                    .reduce(Maven.DepsFilter::or)
                    .orElse(Maven.DepsFilter.trueFilter());

            final Maven.Args build = Maven.Args.builder()
                    .deps(deps)
                    .cmd(asList("clean", "install"))
                    .depsFilter(filter)
                    .build();

            env.executeOffline(
                    project,
                    build
            );

            project.save(
                    env,
                    jarOutput,
                    archiveOutput
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
            final Maven env = Maven.prepareEnv();

            final List<Maven.Project> projects = readLines(configFile).stream()
                    .map(Maven::createProject)
                    .collect(Collectors.toList());
            final Maven.Args build = Maven.Args.builder()
                    .cmd(asList("clean", "dependency:go-offline", "install"))
                    .build();

            env.execute(
                    projects,
                    build
            );

            long size = IOUtils.tarRepositoryRecursive(
                    env,
                    output
            );

            Logs.info(" " + IntStream.range(0,48).mapToObj(i -> "-").collect(Collectors.joining()));
            Logs.info("Build finished. Archived repository " + FileUtils.byteCountToDisplaySize(size));
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