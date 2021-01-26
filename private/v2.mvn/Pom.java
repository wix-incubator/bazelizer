package tools.jvm.v2.mvn;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Streams;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import com.jcabi.xml.XPathContext;
import lombok.Getter;
import lombok.SneakyThrows;
import org.cactoos.Input;
import org.cactoos.Text;
import org.cactoos.io.InputOf;
import org.cactoos.text.TextOf;
import org.xembly.Directive;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class Pom {

    public static final String NAMESPACE_XPATH = "/*/namespace::*[name()='']";
    public static final  String POM_NS_URI = "http://maven.apache.org/POM/4.0.0";
    public static final  String POM_NS = "pom";

    /**
     * Global xpath context.
     */
    public static final  XPathContext XPATH_CONTEXT = new XPathContext()
            .add(POM_NS, POM_NS_URI)
            .add("bz", "https://github.com/wix-incubator/bazelizer");


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
     * @param dir
     * @return pom
     */
    public abstract Pom withDirectives(PomXe... dir);


    /**
     * Group id
     * @return str
     */
    public final String groupId() {
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
    public final String artifactId() {
        return xml().xpath("/project/artifactId/text()").get(0);
    }

    /**
     * Version
     * @return str
     */
    public final String version() {
        return xml().xpath("/project/version/text()").get(0);
    }


    public Text asText() {
        return new TextOf(
                new XemblerXML(xml()).toString()
        );
    }


    @SuppressWarnings("Guava")
    static class Std extends Pom {

        public Std(Input in) {
            this(() -> get(in));
        }

        public Std(Supplier<XML> input) {
            this.input = Suppliers.memoize(input);
        }

        private final Supplier<XML> input;

        @Override
        public XML xml() {
            return input.get();
        }

        @SneakyThrows
        private static XML get(Input i) {
            try (InputStream is = i.stream()) {
                return new XemblerXML(new XMLDocument(is));
            }
        }

        @SuppressWarnings("UnstableApiUsage")
        @Override
        public Pom withDirectives(PomXe... dir) {
            return new Std(() -> {
                final XML xml = xml();
                final Iterable<Directive> dirs = Stream.of(dir)
                        .flatMap(pomDir -> Streams.stream(pomDir.apply(xml)))
                        .collect(Collectors.toList());
                return XemblerAug.applyQuietly(xml, XPATH_CONTEXT, dirs);
            });
        }
    }
}
