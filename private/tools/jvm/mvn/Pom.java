package tools.jvm.mvn;

import com.google.common.collect.Sets;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Delegate;
import org.cactoos.Input;
import org.cactoos.io.InputOf;
import org.cactoos.text.TextOf;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public interface Pom {

    @Data
    class Props {
        final java.lang.String groupId;
        final java.lang.String artifactId;
        final java.lang.String version;
    }


    default Set<java.lang.String> namespaces() throws Exception {
        return Collections.emptySet();
    }

    /**
     * Properties of pom file.
     * @return props
     */
    default Props props() throws Exception  {
        throw new IllegalStateException();
    }

    /**
     * Pom as XMP
     * @return xml
     * @throws Exception if any error
     */
    XML xml() throws Exception ;

    /**
     * File source.
     * @return source
     */
    default File source() {
        throw new UnsupportedOperationException("source()");
    }


    @AllArgsConstructor
    abstract class Wrap implements Pom {
        @Delegate
        private final Pom pom;
    }

    @ToString
    class PomOf extends Wrap implements Pom {

        private final File file;


        public PomOf(Path _file) {
            this(_file.toFile());
        }

        public PomOf(File _file) {
            super(new StringOf(new InputOf(_file)));
            file = _file;
        }

        @Override
        public File source() {
            return file;
        }
    }

    class StringOf implements Pom {

        /**
         * Real file.
         */
        private final Input input;


        public StringOf(java.lang.String input) {
            this(new InputOf(new TextOf(input)));
        }

        public StringOf(Input input) {
            this.input = input;
        }


        @Override
        public Set<java.lang.String> namespaces() throws Exception {
            return Sets.newHashSet(
                    xml().xpath("/*/namespace::*[name()='']")
            );
        }

        @Override
        public Props props() throws Exception {
            XML xml = xml();
            final List<java.lang.String> namespaces = xml.xpath("/*/namespace::*[name()='']");
            if (!namespaces.isEmpty()) {
                java.lang.String gid =  xml.xpath("/pom:project/pom:groupId/text()").get(0);
                java.lang.String aid =  xml.xpath("/pom:project/pom:artifactId/text()").get(0);
                java.lang.String v =  xml.xpath("/pom:project/pom:version/text()").get(0);
                return new Props(gid, aid, v);
            } else {
                java.lang.String gid =  xml.xpath("/project/groupId/text()").get(0);
                java.lang.String aid =  xml.xpath("/project/artifactId/text()").get(0);
                java.lang.String v =  xml.xpath("/project/version/text()").get(0);
                return new Props(gid, aid, v);
            }
        }

        @Override
        public XML xml() throws Exception {
            try (InputStream src = input.stream()) {
                return new XMLDocument(src)
                        .registerNs("xe", "http://www.w3.org/1999/xhtml") // dummy
                        .registerNs("pom", "http://maven.apache.org/POM/4.0.0");
            }
        }
    }
}
