package tools.jvm.v2.mvn;

import com.google.common.base.Joiner;
import com.google.common.hash.Hashing;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.cactoos.io.InputOf;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

public interface Dep {

    static Collection<Dep> load(Manifest manifest) {
        return manifest.lines()
                .stream()
                .map(json -> Main.GSON.fromJson(json, DepInfo.class).toDep())
                .collect(Collectors.toSet());
    }

    /**
     * Group Id.
     */
    String getGroupId();

    /**
     * Artifact Id.
     */
    String getArtifactId();

    /**
     * Version.
     */
    String getVersion();

    /**
     * Jar.
     *
     * @return jar
     */
    Path getSource();

    /**
     * Scope
     *
     * @return scope
     */
    default String scope() {
        return "compile";
    }


    interface Install {
        void exec(Dep dep, Path artifactFolder) throws Exception;

        default String resolveMvnFileName(Dep dep) {
            return dep.getArtifactId() + "-" + dep.getVersion();
        }
    }

    /**
     * Installation.
     */
    default Install install() {
        return new FolderFor(
                new AllOf(
                        new PomFor(),
                        new JarFor()
                ));
    }

    @ToString(of = {"groupId", "artifactId", "version"})
    @Data
    @EqualsAndHashCode(of = {"groupId", "artifactId", "version"})
    class Simple implements Dep {
        private final Path source;
        private final String groupId;
        private final String artifactId;
        private final String version;
    }

    @ToString(callSuper = true)
    class FromArtifact extends Simple {
        public FromArtifact(Path source, String groupId, String artifactId, String version) {
            super(source, groupId, artifactId, version);
        }

        @Override
        public Install install() {
            return new AllOf(Arrays.asList(
                    new Untar(getSource()),
                    new FolderFor(
                            new PomFor()
                    )
            ));
        }
    }


    @SuppressWarnings({"DuplicatedCode", "UnstableApiUsage"})
    static Dep hashed(Path file) {
        String filePath = file.toAbsolutePath().toString();
        String hash = Hashing
                .murmur3_128()
                .hashString(filePath, StandardCharsets.UTF_8)
                .toString();
        String groupId = "io.bazelbuild." + hash;
        String artifactId = file.getFileName().toString()
                .replace("/", "_")
                .replace("=", "_")
                .replace(".jar", "");
        String version = "rev-" + hash.substring(0, 7);
        return new Simple(file, groupId, artifactId, version);
    }


    @NoArgsConstructor
    @EqualsAndHashCode(of = {"path", "tags"})
    @Getter
    @Setter
    class DepInfo {
        private Path path;
        private Map<String, String> tags = Collections.emptyMap();

        @SuppressWarnings("DuplicatedCode")
        public Dep toDep() {
            final String extension = FilenameUtils
                    .getExtension(path.getFileName().toString());

            // regular jar, install hashed coordinates
            if (extension.endsWith("jar")) {
                return hashed(path);
            }

            // archived maven installed folder, try unpack and install it as is.
            if (extension.endsWith("tar")) {
                return TarUtils.list(path).stream()
                        .filter(name -> name.endsWith(".jar"))
                        .map(pathWithinTar -> {
                            final List<String> parts = Arrays.asList(pathWithinTar.split("/"));
                            final String version = parts.get(parts.size() - 2);
                            final String art = parts.get(parts.size() - 3);
                            final String gid = Joiner.on(".").join(parts.subList(0, parts.size() - 3));
                            return new FromArtifact(path, gid, art, version);
                        })
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("tar has not resolvable content in "
                                + path + ": " + TarUtils.list(path)));
            }
            throw new IllegalStateException("not supported extension for " + path);
        }
    }

    @AllArgsConstructor
    class AllOf implements Install {
        private final List<Install> list;

        public AllOf(Install... i) {
            this(Arrays.asList(i));
        }

        @Override
        public void exec(Dep dep, Path repo) throws Exception {
            for (Install install : list) {
                install.exec(dep, repo);
            }
        }
    }

    @AllArgsConstructor
    class FolderFor implements Install {
        private final Install proc;

        @Override
        public void exec(Dep dep, Path repository) throws Exception {
            final Path artifactFolder = repository.resolve(Mvn.mvnLayout(dep));
            //noinspection ResultOfMethodCallIgnored,DuplicatedCode
            artifactFolder.toFile().mkdirs();
            proc.exec(dep, artifactFolder);
        }
    }

    @AllArgsConstructor
    @Slf4j
    class JarFor implements Install {

        @Override
        public void exec(Dep dep, Path artifactFolder) throws Exception {
            String fileName = resolveMvnFileName(dep);
            Path jarFile = artifactFolder.resolve(fileName + ".jar");
            log.info("[{}] writing jar: {}", dep, jarFile);
            Files.copy(dep.getSource(), jarFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @AllArgsConstructor
    @Slf4j
    class PomFor implements Install {

        @Override
        public void exec(Dep dep, Path artifactFolder) throws Exception {
            String fileName = resolveMvnFileName(dep);
            Path pomFile = artifactFolder.resolve(fileName + ".pom");
            String pom = "<project>\n" +
                    "<modelVersion>4.0.0</modelVersion>\n" +
                    "<groupId>" + dep.getGroupId() + "</groupId>\n" +
                    "<artifactId>" + dep.getArtifactId() + "</artifactId>\n" +
                    "<version>" + dep.getVersion() + "</version>\n" +
                    "<description>Generated for " + dep + "</description>\n" +
                    "</project>";
            log.info("[{}] writing pom:\n{}", dep, pom);
            Files.write(pomFile, pom.getBytes(StandardCharsets.UTF_8));
        }
    }

    @AllArgsConstructor
    @Slf4j
    class Untar implements Install {
        private final Path dep;

        @Override
        public void exec(Dep dep, Path repo) throws Exception {
            log.info("[{}}] untar artifact: {}", dep, dep.getSource());
            TarUtils.untar(new InputOf(dep.getSource()), repo);
        }
    }
}
