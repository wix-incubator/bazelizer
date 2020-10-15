package tools.jvm.mvn;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.reflect.Invokable;
import com.jcabi.xml.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.cactoos.Text;
import org.cactoos.io.InputOf;
import org.cactoos.text.TextOf;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public interface Template {

    /**
     * Evaluate template.
     * @return char source
     */
    Text eval();

    /**
     * Mustache template with data resolved from pom.xml
     */
    class PomXPath extends Mustache implements Template {

        @Data
        static class Props {
            final String groupId;
            final String artifactId;
            final String version;
        }

        @SuppressWarnings("UnstableApiUsage")
        public PomXPath(String source, File pom) {
            super(CharSource.wrap(source).asByteSource(StandardCharsets.UTF_8), props(new InputOf(pom)));
        }

        @SuppressWarnings("UnstableApiUsage")
        public PomXPath(String source, String pom) {
            super(CharSource.wrap(source).asByteSource(StandardCharsets.UTF_8), props(new InputOf(pom)));
        }

        @SneakyThrows
        private static Props props(InputOf xmlIn) {
            try (InputStream src = xmlIn.stream()) {
//                final Constructor<XMLDocument> constructor = XMLDocument.class
//                        .getDeclaredConstructor(Node.class, XPathContext.class, boolean.class);
//                constructor.setAccessible(true);
//                final Field field = XMLDocument.class.getDeclaredField("DFACTORY");
//                field.setAccessible(true);
//                final DocumentBuilderFactory dfactory = (DocumentBuilderFactory) field.get(XMLDocument.class);
//
//                final Invokable<XMLDocument, XMLDocument> invokable = Invokable.from(constructor);
//                XML xml = invokable.invoke(null,
//                        dfactory.newDocumentBuilder().parse(src),
//                        new XPathContext().add("pom", "http://maven.apache.org/POM/4.0.0"),
//                        false
//                );


                XML xml = new XMLDocument(src).registerNs("pom", "http://maven.apache.org/POM/4.0.0");

                if (!xml.xpath("/*/namespace::*[name()='']").isEmpty()) {
                    String gid =  xml.xpath("/pom:project/pom:groupId/text()").get(0);
                    String aid =  xml.xpath("/pom:project/pom:artifactId/text()").get(0);
                    String v =  xml.xpath("/pom:project/pom:version/text()").get(0);
                    return new Props(gid, aid, v);
                } else {
                    String gid =  xml.xpath("/project/groupId/text()").get(0);
                    String aid =  xml.xpath("/project/artifactId/text()").get(0);
                    String v =  xml.xpath("/project/version/text()").get(0);
                    return new Props(gid, aid, v);
                }
            }
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
