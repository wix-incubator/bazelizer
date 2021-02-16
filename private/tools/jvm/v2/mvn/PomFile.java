package tools.jvm.v2.mvn;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.cactoos.io.InputOf;
import org.xembly.Directive;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public interface PomFile {
    /**
     * Pom.
     * @return pom
     */
    Pom pom();

    /**
     * Update.
     * @param upd upd
     * @return this.
     */
    PomFile update(PomUpdate...upd);


    /**
     * Pom file location.
     * @return location
     */
    default File persisted() {
        return persisted(true);
    }

    /**
     * Pom file location.
     * @return location
     */
    File persisted(boolean write);


    default Optional<Path> parent() {
        return Optional.empty();
    }

    @Slf4j
    class Simple implements PomFile {
        private Pom pom;
        private final File src;
        private final File dest;

        public Simple(File src) {
            this.pom = new Pom.Std(new InputOf(src));
            this.src = src;
            this.dest = src.toPath().getParent().resolve("pom." + Mvn.LABEL.value() + ".xml").toFile();
        }

        @Override
        public Pom pom() {
            return pom;
        }

        @Override
        public PomFile update(PomUpdate... upd) {
            this.pom = this.pom.update(upd);
            return this;
        }

        @Override
        public Optional<Path> parent() {
            return pom.relativePath().map(rel ->
                    src.toPath().getParent().resolve(rel).normalize());
        }

        @SneakyThrows
        @Override
        public File persisted(boolean w) {
            this.parent().ifPresent(p -> {
                final Path abs = p.toAbsolutePath();
                if (Files.notExists(abs)) {
                    log.error("[pom.xml] this pom file referenced to the relative " +
                            "parent file that doesn't exists; {}",p);
                }
            });
            if (!dest.exists() && w) {
                Files.write(dest.toPath(), pom.bytes().asBytes());
                if (log.isInfoEnabled()) {
                    log.info("[pom.xml] persist {}", dest);
                    log.info("{}", pom.asString());
                }
                return dest;
            }
            return dest;
        }
    }
}
