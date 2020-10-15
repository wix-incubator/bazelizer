package tools.jvm.mvn;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.hash.Hashing;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Extenral dependency.
 */
public interface Dep {

    String IS_FULL_ARTIFACT_DIST_TAG = "artifact";


    /**
     * Group Id.
     */
    String groupId();

    /**
     * Artifact Id.
     */
    String artifactId();

    /**
     * Version.
     */
    String version();

    /**
     * Jar.
     * @return jar
     */
    Path source();

    /**
     * Maven packaging type.
     * @return only jar for now
     */
    default Map<String,String> tags() {
        return Collections.emptyMap();
    }


    /**
     * Resolve maven layout, relative to repo
     * @param repo relative to this
     * @return artifact folder
     */
    default Path relativeTo(Path repo) {
        String[] gidParts = this.groupId().split("\\.");
        Path thisGroupIdRepo = repo;
        for (String gidPart : gidParts) {
            thisGroupIdRepo = thisGroupIdRepo.resolve(gidPart);
        }
        return thisGroupIdRepo.resolve(this.artifactId()).resolve(this.version());
    }


    /**
     * Dependency resolved .
     */
    class DependencyOf extends Wrap {

        /**
         * Ctro.
         * @param artifact artifact
         */
        public DependencyOf(Deps.DepArtifact artifact) {
            super(build(artifact));
        }

        @SuppressWarnings("StaticPseudoFunctionalStyleMethod")
        @SneakyThrows
        private static Dep build(Deps.DepArtifact artifact) {
            if (artifact.getTags().containsKey(IS_FULL_ARTIFACT_DIST_TAG)) {
                final Path path = artifact.getPath();
                @SuppressWarnings("ConstantConditions")
                final TarArchiveEntry archiveEntry = Iterables.find(new Archive.TarList(path),
                        entry -> String.valueOf(entry.getName()).endsWith(".jar"), null);
                if (archiveEntry != null) {
                    final String name = archiveEntry.getName();
                    //example: com/mavenizer/examples/api/myapi-single/1.0.0-SNAPSHOT/myapi-single-1.0.0-SNAPSHOT.jar
                    return fromTarEntry(artifact.getTags(), path, name);
                }
            }

            return new Dep.DigestCoords(artifact);
        }

        public static Dep fromTarEntry(Map<String,String> tags, Path tar, String artifactPath) {
            final List<String> parts = Arrays.asList(artifactPath.split("/"));
            final String version = parts.get(parts.size() - 2);
            final String art = parts.get(parts.size() - 3);
            final String gid = Joiner.on(".").join(parts.subList(0, parts.size() - 3));
            return new Simple(tar.toFile(), gid, art, version, tags);
        }
    }



    @AllArgsConstructor
    @ToString
    class Wrap implements Dep {
        @Delegate
        private final Dep dep;
    }

    @EqualsAndHashCode(of = {"gid", "aid", "version"})
    @ToString
    class Simple implements Dep {
        private final File file;
        private final String gid;
        private final String aid;
        private final String version;
        private final Map<String,String> tags;

        public Simple(File file, String gid, String aid, String v, Map<String, String> tags) {
            this.file = file;
            this.version = v;
            this.gid = gid;
            this.aid = aid;
            this.tags = tags;
        }

        public Simple(File file, String gid, String aid, String v) {
            this(file, gid, aid, v, Collections.emptyMap());
        }

        @Override
        public String groupId() {
            return gid;
        }

        @Override
        public String artifactId() {
            return aid;
        }

        @Override
        public String version() {
            return version;
        }

        @Override
        public Path source() {
            return file.toPath();
        }
    }


    @EqualsAndHashCode(of = {"orig"})
    @ToString
    class DigestCoords implements Dep {
        private final Dep orig;

        @SneakyThrows
        public DigestCoords(Deps.DepArtifact jarFile) {
            this.orig = create(jarFile.getPath().toFile(), jarFile.getTags());
        }

        @Override
        public Map<String, String> tags() {
            return this.orig.tags();
        }

        @Override
        public String groupId() {
            return orig.groupId();
        }

        @Override
        public String artifactId() {
            return orig.artifactId();
        }

        @Override
        public String version() {
            return orig.version();
        }

        @Override
        public Path source() {
            return orig.source();
        }

        @SuppressWarnings("UnstableApiUsage")
        private static Dep create(File jarFile, Map<String,String> tags) throws URISyntaxException {
            String filePath = jarFile.getPath();
            String hash = Hashing
                    .murmur3_128()
                    .hashString(filePath, StandardCharsets.UTF_8)
                    .toString();
            String groupId = "io.bazelbuild." + hash;
            String artifactId = jarFile.getName()
                    .replace("/", "_")
                    .replace("=", "_")
                    .replace(".jar", "");
            String version = "rev-" + hash.substring(0, 7);
            return new Simple(jarFile, groupId, artifactId, version, tags);
        }
    }

}
