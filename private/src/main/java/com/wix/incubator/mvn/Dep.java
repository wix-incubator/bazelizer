package com.wix.incubator.mvn;

import com.google.common.base.Joiner;
import com.google.common.hash.Hashing;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;

/**
 * Dependency from bazel
 */
public abstract class Dep {

    private static class DepDTO {
        public Path file;
        public Map<String, String> tags;
    }

    /**
     * New dep.
     *
     * @param jsonDef json definition
     * @return a dep
     */
    public static Dep fromJson(String jsonDef) {
        DepDTO dto = Cli.GSON.fromJson(jsonDef, DepDTO.class);
        final Path file = dto.file;
        final String extension = FilenameUtils.getExtension(file.getFileName().toString());
        switch (extension) {
            case "jar":
                return new Bazel(dto);
            case "tar":
                return new Tar(file);
            default:
                throw new IllegalArgumentException("not supported dep from a file " + file);
        }
    }

    public final String groupId;
    public final String artifactId;
    public final String version;

    public String scope() {
        return "compile";
    }

    /**
     * Ctor.
     *
     * @param id id
     */
    protected Dep(String[] id) {
        this(id[0], id[1], id[2]);
    }

    /**
     * Ctor.
     */
    private Dep(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    /**
     * Install dep into local repository.
     *
     * @param repo repo location
     * @throws IOException if any
     */
    public abstract void installTo(Path repo) throws IOException;


    @SuppressWarnings("UnstableApiUsage")
    private static class Bazel extends Dep {
        private final Path sourceFile;
        private final String scope;

        private Bazel(DepDTO struct) {
            super(mkVersion(struct));
            this.sourceFile = struct.file;
            this.scope = Optional.ofNullable(struct.tags)
                    .map(t -> t.get("scope")).orElse("compile");
        }

        @Override
        public String scope() {
            return scope;
        }

        @Override
        public void installTo(Path repo) throws IOException {
            final Path depFolder = repo.resolve(Maven.artifactRepositoryLayout(groupId, artifactId, version));
            Files.createDirectories(depFolder);
            String fileName = this.artifactId + "-" + this.version;
            Files.copy(this.sourceFile, depFolder.resolve(fileName + ".jar"), StandardCopyOption.REPLACE_EXISTING);
            writePom(this, depFolder);
        }

        private static String[] mkVersion(DepDTO struct) {
            String filePath = struct.file.toAbsolutePath().toString();
            String hash = Hashing.murmur3_128().hashString(filePath, StandardCharsets.UTF_8).toString();
            String groupId = "bazelizer." + hash;
            String artifactId = struct.file.getFileName().toString()
                    .replace("/", "_")
                    .replace("=", "_")
                    .replace(".jar", "")
                    .replace(".", "_");
            String version = "rev-" + hash.substring(0, 7);
            return new String[]{groupId, artifactId, version};
        }
    }


    private static class Tar extends Dep {
        private final Path tar;

        protected Tar(Path source) {
            super(readCoords(source));
            tar = source;
        }

        @Override
        public void installTo(Path repo) throws IOException {
            IOSupport.untar(tar, repo);
            final Path depFolder = repo.resolve(Maven.artifactRepositoryLayout(groupId, artifactId, version));
            Dep.writePom(this, depFolder); // override pom by synthetic
        }

        private static String[] readCoords(Path source) {
            return IOSupport.lisTartUnchacked(source).stream()
                    .filter(name -> name.endsWith(".jar"))
                    .findFirst()
                    .map(pathWithinTar -> {
                        final List<String> parts = asList(pathWithinTar.split("/"));
                        final String version = parts.get(parts.size() - 2);
                        final String art = parts.get(parts.size() - 3);
                        final String gid = Joiner.on(".").join(parts.subList(0, parts.size() - 3));
                        return new String[]{gid, art, version};
                    }).orElseThrow(() -> new IllegalStateException("tar has not resolvable content in "
                            + source + ": " + IOSupport.lisTartUnchacked(source)));
        }
    }

    private static void writePom(Dep dep, Path folder) throws IOException {
        String pomXml = "<project>\n" +
                "   <modelVersion>4.0.0</modelVersion>\n" +
                "   <groupId>" + dep.groupId + "</groupId>\n" +
                "   <artifactId>" + dep.artifactId + "</artifactId>\n" +
                "   <version>" + dep.version + "</version>\n" +
                "   <description>generated</description>\n" +
                "</project>";

        Files.write(folder.resolve((dep.artifactId + "-" + dep.version) + ".pom"),
                pomXml.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }
}
