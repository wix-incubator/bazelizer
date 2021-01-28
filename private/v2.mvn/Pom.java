package tools.jvm.v2.mvn;

import com.google.common.collect.Streams;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import com.jcabi.xml.XPathContext;
import lombok.SneakyThrows;
import org.cactoos.Input;
import org.cactoos.Scalar;
import org.cactoos.io.InputOf;
import org.xembly.Directive;

import java.io.File;
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


    interface EffectFS {
        File asFile();
    }


    public static Pom open(Path absFile) {
        return new Pom.Std(new InputOf(absFile));
    }

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
     * File system effect.
     * @return fs
     */
    public abstract EffectFS toEffect();

    /**
     * Update pom.
     * @param dir dirs
     * @return pom
     */
    @SuppressWarnings("UnstableApiUsage")
    public Pom withDirectives(PomUpdate... dir) {
        return new Std(() -> {
            final XML xml = xml();
            final Iterable<Directive> dirs = Stream.of(dir)
                    .flatMap(pomDir -> Streams.stream(pomDir.apply(this)))
                    .collect(Collectors.toList());
            return XemblerAug.applyQuietly(xml, XPATH_CONTEXT, dirs);
        });
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


    static class Std extends Pom {

        public Std(Input in) {
            this(() -> get(in));
        }

        public Std(Scalar<XML> input) {
            this.input = Maven.memoize(input);
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
                return new XemblerXML(new XMLDocument(is));
            }
        }

        @Override
        public EffectFS toEffect() {
            return null;
        }
    }
}
