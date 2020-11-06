package tools.jvm.mvn;

import com.google.common.io.Closer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.commons.io.IOUtils;
import org.cactoos.*;
import org.cactoos.func.UncheckedFunc;
import org.cactoos.func.UncheckedProc;
import org.cactoos.io.InputOf;
import org.cactoos.io.OutputTo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

/**
 * Output.
 */
public interface OutputFile {

    /**
     * Write declared output for the project.
     * @param project project
     */
    void exec(Project project);


    @AllArgsConstructor
    @ToString
    class TargetFolderFile implements OutputFile {
        private final String src;
        private final String dest;

        /**
         * Write declared output for the project.
         * @param project project
         */
        public void exec(Project project)  {
            final Path workDir = project.workDir();
            final Path target = workDir.resolve("target").toAbsolutePath();
            Path src = target.resolve(this.src);
            Path dest = Paths.get(this.dest);
            try {
                Files.copy(src, dest);
            } catch (java.nio.file.NoSuchFileException e) {
                throw new ToolOutputNotFoundException(src, target, e);
            } catch (IOException e) {
                throw new ToolException(e);
            }
        }
    }

    @AllArgsConstructor
    @ToString
    class Content implements OutputFile {
        private final Input src;
        private final Output dest;

        public Content(Bytes src, Output dest) {
            this(new InputOf(src), dest);
        }

        public Content(Input src, Path dest) {
            this(src, new OutputTo(dest));
        }

        /**
         * Write declared output for the project.
         * @param project project
         */
        @SuppressWarnings("UnstableApiUsage")
        @SneakyThrows
        public void exec(Project project)  {
            try (Closer closer = Closer.create()) {
                IOUtils.copy(closer.register(src.stream()), closer.register(dest.stream()));
            } catch (IOException e) {
                throw new ToolException(e);
            }
        }
    }

    @AllArgsConstructor
    @ToString
    class ArchiveOf implements OutputFile {
        private final Archive src;
        private final Output dest;

        /**
         * Write declared output for the project.
         * @param project project
         */
        @SneakyThrows
        public void exec(Project project)  {
            src.writeTo(dest);
        }
    }
}

