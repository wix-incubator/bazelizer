package tools.jvm.mvn;

import com.google.common.hash.Hashing;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.ToString;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * Extenral dependency.
 */
public interface Dep {

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
        public DigestCoords(FilePaths.Target jarFile) {
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
