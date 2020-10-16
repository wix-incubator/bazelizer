package tools.jvm.mvn;

import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Streams;
import com.google.common.hash.Hashing;
import lombok.*;
import lombok.experimental.Delegate;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Extenral dependency.
 */
public interface Dep {

    Logger log = LoggerFactory.getLogger(Dep.class);


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
     * Scope
     * @return scope
     */
    default String scope() {
        return "compile";
    }

    /**
     * Maven packaging type.
     * @return only jar for now
     */
    default Map<String,String> tags() {
        return Collections.emptyMap();
    }



    @SuppressWarnings("unused")
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(of = {"path", "tags"})
    class DepArtifact implements Dep {
        private Path path;
        private Map<String, String> tags = Collections.emptyMap();

        @SuppressWarnings("Guava")
        @Getter(AccessLevel.PRIVATE)
        private final Supplier<Dep> cached = Suppliers.memoize(this::original);

        /**
         * Ctor. for a path only.
         * @param p path
         */
        public DepArtifact(Path p) {
            this.path = p;
        }

        @Override
        public String groupId() {
            return cached.get().groupId();
        }

        @Override
        public String artifactId() {
            return cached.get().artifactId();
        }

        @Override
        public String version() {
            return cached.get().version();
        }

        @Override
        public Path source() {
            return path;
        }

        @Override
        public Install installTo() {
            return cached.get().installTo();
        }

        @Override
        public Map<String, String> tags() {
            return tags;
        }

        @Override
        public String scope() {
            return tags.getOrDefault("scope", "compile");
        }

        @SuppressWarnings("UnstableApiUsage")
        @SneakyThrows
        private Dep original() {
            final String extension = FilenameUtils.getExtension(path.getFileName().toString());
            if (extension.endsWith("jar")) {
                // regular jar, install hashed coordinates
                return new Dep.DigestCoords(path);
            }
            if (extension.endsWith("tar")) {
                // installed artifact as it was
                return Streams.stream(new Archive.LSTar(path))
                        .filter(entry -> {
                            final String name = entry.getName();
                            return name.endsWith(".jar");
                        }).map(TarArchiveEntry::getName)
                        .map(artifactPath -> new Archived(path, artifactPath))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("tar has not resolvable content in " + path));
            }
            throw new IllegalStateException("not supported extension for " + path);
        }

    }


    /**
     * Install
     */
    @SneakyThrows
    default Install installTo() {
        return Install.all(
                new Install.Folder(this),
                new Install.NewPom(this),
                new Install.NewJar(this)
        );
    }

    /**
     * Folder of current dep
     * @param repo repository
     * @return folder
     */
    default Path artifactFolder(Path repo) {
        return this.relativeTo(repo);
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

        @SuppressWarnings("Guava")
        private Supplier<Path> cachedArtifactFolder;

        @Override
        public Path artifactFolder(Path repo) {
            if (cachedArtifactFolder == null) {
                cachedArtifactFolder = Suppliers.memoize(() -> {
                    return Dep.super.artifactFolder(repo);
                });
            }
            return cachedArtifactFolder.get();
        }

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
            return gid.trim();
        }

        @Override
        public String artifactId() {
            return aid.trim();
        }

        @Override
        public String version() {
            return version.trim();
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
        public DigestCoords(Path path) {
            this.orig = create(path.toFile());
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
        private static Dep create(File jarFile) {
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
            return new Simple(jarFile, groupId, artifactId, version);
        }
    }



    /**
     * Dependency resolved .
     */
    @ToString(callSuper = true)
    class Archived extends Wrap {

        /**
         * Ctro.
         */
        public Archived(Path tar, String someFileInTar) {
            super(build(tar, someFileInTar));
        }


        @Override
        public Install installTo() {
            return Install.all(
                    new Install.Folder(this),
                    new Install.Untar(this), // don't have a pom file
                    new Install.NewPom(this)
            );
        }

        @SneakyThrows
        private static Dep build(Path tar, String someFileInTar) {
            final List<String> parts = Arrays.asList(someFileInTar.split("/"));
            final String version = parts.get(parts.size() - 2);
            final String art = parts.get(parts.size() - 3);
            final String gid = Joiner.on(".").join(parts.subList(0, parts.size() - 3));
            return new Dep.Simple(tar.toFile(), gid, art, version);
        }
    }
}
