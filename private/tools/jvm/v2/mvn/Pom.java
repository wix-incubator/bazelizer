package tools.jvm.v2.mvn;

import com.google.common.collect.Streams;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import com.jcabi.xml.XPathContext;
import lombok.SneakyThrows;
import org.cactoos.Bytes;
import org.cactoos.Input;
import org.cactoos.Scalar;
import org.cactoos.io.BytesOf;
import org.xembly.Directive;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Pom {

    public static final  String POM_NS_URI = "http://maven.apache.org/POM/4.0.0";
    public static final  String POM_NS = "pom";

    public static final String NS = "bz";
    public static final String NS_URI = "https://github.com/wix-incubator/bazelizer";

    /**
     * Global xpath context.
     */
    public static final  XPathContext XPATH_CONTEXT = new XPathContext()
            .add(POM_NS, POM_NS_URI)
            .add(NS, NS_URI);


    /**
     * Parent pom.
     * @return maybe pom
     */
    public Optional<String> relativePath() {
        final XML xml = xml();
        List<String> text = xml.xpath("/project/parent/relativePath/text()");
        if (text.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(text.get(0));
    }

    /**
     * Pom as XMP
     * @return xml
     */
    public abstract XML xml();

    /**
     * Update pom.
     * @param dirs dirs
     * @return pom
     */
    public Pom update(Iterable<Directive> dirs) {
        return new Std(() -> {
            final XML xml = xml();
            return XemblerAug.applyQuietly(xml, XPATH_CONTEXT, dirs);
        });
    }

    /**
     * Helper function to update.
     * @param upd pom updates
     * @return new pom
     */
    public Pom update(PomUpdate... upd) {
        final Collection<Directive> dirs = new ArrayList<>();
        for (PomUpdate u : upd) {
            u.apply(this).forEach(dirs::add);
        }
        return this.update(dirs);
    }

    /**
     * Group id
     * @return str
     */
    public  String groupId() {
        final XML xml = xml();
        List<String> text = xml.xpath("/project/groupId/text()");
        if (text.isEmpty()) {
            text = xml.xpath("/project/parent/groupId/text()");
        }
        return text.get(0);
    }

    /**
     * Artifact id
     * @return str
     */
    public  String artifactId() {
        return xml().xpath("/project/artifactId/text()").get(0);
    }

    /**
     * Version
     * @return str
     */
    public  String version() {
        return xml().xpath("/project/version/text()").get(0);
    }


    /**
     * Artifact location in repo.
     * @return path
     */
    public Path folder() {
        return Mvn.mvnLayout(new Dep.Simple(null, groupId(), artifactId(), version()));
    }

    /**
     * XML String.
     * @return str
     */
    String asString() {
        return xml().toString();
    }

    /**
     * XML String.
     * @return str
     */
    Bytes bytes() {
        return new BytesOf(xml().toString());
    }

    /**
     * XML based.
     */
    static class Std extends Pom {

        public Std(Input in) {
            this(() -> get(in));
        }

        public Std(Scalar<XML> input) {
            this.input = Mvn.memoize(input);
        }

        private final Scalar<XML> input;

        @SneakyThrows
        @Override
        public XML xml() {
            return input.value();
        }

        @SneakyThrows
        private static XML get(Input i) {
            try (InputStream is = i.stream()) {
                return new XemblerXML(new XMLDocument(is).merge(XPATH_CONTEXT));
            }
        }
    }
}
