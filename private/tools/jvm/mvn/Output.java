package tools.jvm.mvn;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Output.
 */
public interface Output {

    String DEFAULT_TARGET_JAR_OUTPUT = "@@TARGET-JAR-OUTPUT@@";

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



    class Paths implements Output {
        private final Supplier<String> src;
        private final String dest;

        public Paths(String src, String dest, Project project) {
            this.dest = resolve(dest);
            this.src = Memento.memorize(() -> new TemplateEnvelop.PomBased(resolve(src), project.lazy()).eval().read());
        }

        private String resolve(String src) {
            if (src.contains(DEFAULT_TARGET_JAR_OUTPUT)) {
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


    @AllArgsConstructor
    class Jar implements Output {
        final Output output;

        @Override
        public String src() {
            final String src = output.src();
            if (src.contains("@@MVN@@")) {
                final int from = src.lastIndexOf("@MVN:IF@");
                final int end = src.lastIndexOf("@MVN:END@");
                final String substring = src.substring(from, end);
            }
            return null;
        }

        @Override
        public String dest() {
            return null;
        }
    }

    class TmpSrc implements Output {
        private final Path dest;
        private final Path src;

        @SneakyThrows
        public TmpSrc(Path dest) {
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
