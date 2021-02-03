package tools.jvm.v2.mvn;

import lombok.SneakyThrows;
import org.cactoos.io.InputOf;
import org.xembly.Directive;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;

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
    File persisted(boolean write);


    class Just implements PomFile {
        private Pom pom;
        private final File dest;

        public Just(File src) {
            this.pom = new Pom.Std(new InputOf(src));
            this.dest = src.toPath().getParent().resolve("pom." + Mvn.LABEL + ".xml").toFile();
        }

        @Override
        public Pom pom() {
            return pom;
        }

        @Override
        public PomFile update(PomUpdate... upd) {
            final Collection<Directive> dirs = new ArrayList<>();
            for (PomUpdate u : upd) {
                u.apply(pom).forEach(dirs::add);
            }
            this.pom = this.pom.update(dirs);
            return this;
        }

        @SneakyThrows
        @Override
        public File persisted(boolean w) {
            if (!dest.exists() && w) {
                Files.write(dest.toPath(), pom.bytes().asBytes());
                return dest;
            }

            return dest;
        }
    }
}
