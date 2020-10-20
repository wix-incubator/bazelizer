package tools.jvm.mvn;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import org.cactoos.Output;
import org.cactoos.Proc;
import org.cactoos.func.UncheckedProc;
import org.cactoos.io.OutputTo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Output.
 */
public interface OutputFile {

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
     * Write declired output for a project.
     * @param project project
     */
    default void exec(Project project)  {
        OutputFile name = this;
        final Path workDir = project.workDir();
        final Path target = workDir.resolve("target").toAbsolutePath();
        Path src = target.resolve(name.src());
        Path dest = Paths.get(name.dest());
        try {
            Files.copy(src, dest);
        } catch (java.nio.file.NoSuchFileException e) {
            throw new ToolOutputNotFoundException(src, target, e);
        } catch (IOException e) {
            throw new ToolException(e);
        }
    }

    /**
     * Default source and destination resolution logic.
     */
    @AllArgsConstructor
    @ToString
    class Simple implements OutputFile {
        private final String src;
        private final String dest;

        @Override
        public String src() {
            return src;
        }

        @Override
        public String dest() {
            return dest;
        }
    }

    /**
     * Declared output of a file.
     */
    @AllArgsConstructor
    @ToString
    class Declared implements OutputFile {
        private final File src;
        private final String dest;

        @Override
        public String src() {
            throw new UnsupportedOperationException("src");
        }

        @Override
        public String dest() {
            return dest;
        }

        @Override
        public void exec(Project project) {
            final Path source = src.toPath();
            try {
                Files.copy(source, Paths.get(dest()));
            } catch (java.nio.file.NoSuchFileException e) {
                throw new ToolOutputNotFoundException(src, source.getParent(), e);
            } catch (IOException e) {
                throw new ToolException(e);
            }
        }
    }


    /**
     * Declared output.
     */
    @AllArgsConstructor
    @ToString
    class DeclaredProc implements OutputFile {
        private final Proc<Output> src;
        private final String dest;

        @Override
        public String src() {
            throw new UnsupportedOperationException("src");
        }

        @Override
        public String dest() {
            throw new UnsupportedOperationException("src");
        }

        @Override
        public void exec(Project project) {
            new UncheckedProc<>(src).exec(new OutputTo(new File(dest)));
        }
    }

    /**
     * Declared tar of the dir.
     */
    class DeclaredTarDir extends DeclaredProc implements OutputFile {

        /**
         * Ctor.
         * @param src dir to archive
         * @param dest dest of archive
         */
        public DeclaredTarDir(Path src, String dest) {
            super(new Archive.TarDirectory(src), dest);
        }
    }


    @ToString
    class TemporaryFileSrc implements OutputFile {
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

