package tools.jvm.v2.mvn;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import lombok.SneakyThrows;
import org.cactoos.io.InputOf;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class Main {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Path.class, (JsonDeserializer<Path>) (json, typeOfT, context) -> {
                final String asString = json.getAsString();
                return Paths.get(asString);
            }).create();

    @CommandLine.Command(name = "run")
    public static class Run implements Runnable {

        @CommandLine.Option(names = {"--pom"}, required = true,
                paramLabel = "POM", description = "the pom xml template file")
        public Path pom;

        @CommandLine.Option(names = {"--m2-repository"},
                paramLabel = "REPO", description = "the repository tar")
        public Path repo;

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
            final Maven maven = new Maven(
                    new InputOf(repo)
            );

            final Build build = maven.buildOf(pom);
            build.addDeps(Dep.deps(new Manifest(deps)));

            build.exec();
        }
    }

    public static void main(String[] args) {

    }
}
