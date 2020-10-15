package tools.jvm.mvn;

import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Output.
 */
public interface Output {

    /**
     * Marker that can be specified in output arguments to indicate that maven
     * build default jar should be resolved.
     */
    String DEFAULT_TARGET_JAR_OUTPUT_MARKER = "@@TARGET-JAR-OUTPUT@@";

    /**
     * Marker for tar for whole installed artifact folder.
     */
    String ARTIFACT_DIR_TARGET_OUTPUT_MARKER = "@@MVN_ARTIFACT_ALL@@";

    /**
     * Maven resulting artifact inside target/
     * @return path
     */
    String src();

    /**
     * Bazel output.
     * @return path
     */
    String dest();

    /**
     * Tags of output.
     * @return tags
     */
    default List<String> tags() {
        return Collections.emptyList();
    }

    /**
     * Default source and destination resolution logic.
     */
    class Default implements Output {
        private final Supplier<String> src;
        private final String dest;

        public Default(String src, String dest, File pom) {
            if (src.contains(DEFAULT_TARGET_JAR_OUTPUT_MARKER)) {
                this.src = Suppliers.memoize(() -> {
                    try {
                        return new Template.PomXPath("{{artifactId}}-{{version}}.jar", pom).eval().asString();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                });
            } else {
                this.src = () -> src;
            }
            this.dest = dest;
        }

        @Override
        public String src() {
            return src.get();
        }

        @Override
        public String dest() {
            return dest;
        }
    }

    class TemporaryFileSrc implements Output {
        private final Path dest;
        private final Path src;

        @SneakyThrows
        public TemporaryFileSrc(Path dest) {
            this.dest = dest;
            this.src = Files.createTempFile("tmp-src", ".dat");
        }

        @Override
        public String src() {
            return src.toString();
        }

        @Override
        public String dest() {
            return dest.toString();
        }
    }
}
