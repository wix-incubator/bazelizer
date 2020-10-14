package tools.jvm.mvn;

import com.google.common.base.Suppliers;
import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
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
     * Default source and destination resolution logic.
     */
    class Default implements Output {
        private final Supplier<String> src;
        private final String dest;

        public Default(String src, String dest, File pom) {
            this.dest = resolve(dest);
            this.src = Suppliers.memoize(() -> {
                try {
                    return new TemplateEnvelop.PomXPathProps(resolve(src), pom).eval().read();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
        }

        private String resolve(String src) {
            if (src.contains(DEFAULT_TARGET_JAR_OUTPUT_MARKER)) {
                return "{{artifactId}}-{{version}}.jar";
            }
            return src;
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
