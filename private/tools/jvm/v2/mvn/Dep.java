package tools.jvm.v2.mvn;

import com.google.common.base.Joiner;
import com.google.common.hash.Hashing;
import lombok.*;
import org.apache.commons.io.FilenameUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
     * @return jar
     */
    Path getSource();

    /**
     * Scope
     * @return scope
     */
    default String scope() {
        return "compile";
    }


    @ToString
    @Data
    @EqualsAndHashCode(of = {"groupId", "artifactId", "version"})
    class Simple implements Dep {
        private final Path source;
        private final String groupId;
        private final String artifactId;
        private final String version;
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
    @Getter @Setter
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
                            return new Simple(path, gid, art, version);
                        })
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("tar has not resolvable content in "
                                + path + ": " + TarUtils.list(path)));
            }
            throw new IllegalStateException("not supported extension for " + path);
        }
    }




}
