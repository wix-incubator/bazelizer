package tools.jvm.mvn;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.jcabi.xml.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.cactoos.Input;
import org.cactoos.Scalar;
import org.cactoos.Text;
import org.cactoos.io.InputOf;
import org.cactoos.text.TextOf;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public interface Template {

    /**
     * Evaluate template.
     * @return char source
     */
    Text eval();


    @Data
    static class PomPropsBean {
        final String groupId;
        final String artifactId;
        final String version;
    }

    @AllArgsConstructor
    class PomPropsBeanXPath implements Scalar<PomPropsBean> {

        private final Input xmlIn;

        @Override
        public PomPropsBean value() throws Exception {
            try (InputStream src = xmlIn.stream()) {
                XML xml = new XMLDocument(src).registerNs("pom", "http://maven.apache.org/POM/4.0.0");
                final List<String> namespaces = xml.xpath("/*/namespace::*[name()='']");
                if (!namespaces.isEmpty()) {
                    String gid =  xml.xpath("/pom:project/pom:groupId/text()").get(0);
                    String aid =  xml.xpath("/pom:project/pom:artifactId/text()").get(0);
                    String v =  xml.xpath("/pom:project/pom:version/text()").get(0);
                    return new PomPropsBean(gid, aid, v);
                } else {
                    String gid =  xml.xpath("/project/groupId/text()").get(0);
                    String aid =  xml.xpath("/project/artifactId/text()").get(0);
                    String v =  xml.xpath("/project/version/text()").get(0);
                    return new PomPropsBean(gid, aid, v);
                }
            }
        }
    }


    /**
     * Mustache template with data resolved from pom.xml
     */
    @SuppressWarnings("UnstableApiUsage")
    class PomXPath extends Mustache implements Template {

        public PomXPath(String source, File pom) {
            super(CharSource.wrap(source).asByteSource(StandardCharsets.UTF_8),
                    valueOf(new PomPropsBeanXPath(new InputOf(pom))));
        }

        public PomXPath(String source, String pom) {
            super(CharSource.wrap(source).asByteSource(StandardCharsets.UTF_8),
                    valueOf(new PomPropsBeanXPath(new InputOf(pom))));
        }

        @SneakyThrows
        private static PomPropsBean valueOf(Scalar<PomPropsBean> xmlIn) {
            return xmlIn.value();
        }
    }


    /**
     * Template envelope via mustache.
     */
    @AllArgsConstructor
    class Mustache implements Template {
        private final ByteSource source;
        private final Object bean;

        @Override
        @SneakyThrows
        public Text eval() {
            final CharSource tplSource = source.asCharSource(StandardCharsets.UTF_8);
            MustacheFactory mf = new DefaultMustacheFactory();
            try (Reader tpl = tplSource.openStream()) {
                final StringWriter str = new StringWriter();
                try (Writer dest = new PrintWriter(str)) {
                    com.github.mustachejava.Mustache mustache = mf.compile(tpl, "template.mustache");
                    mustache.execute(dest, bean);
                }

                return new TextOf(new InputOf(str.toString()));
            }
        }
    }
}
