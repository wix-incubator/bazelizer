package tools.jvm.mvn;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.cactoos.Text;
import org.cactoos.io.InputOf;
import org.cactoos.text.TextOf;
import org.cactoos.text.UncheckedText;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.w3c.dom.Node;
import org.xembly.Directives;
import org.xembly.Xembler;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;

public interface Template {

    /**
     * Evaluate template.
     * @return char source
     */
    Text eval();

    /**
     * Template envelope via mustache.
     */
    @AllArgsConstructor
    class Mustache implements Template {
        private static final MustacheFactory MF = new DefaultMustacheFactory();


        private final ByteSource source;

        private final Object bean;

        @Override
        @SneakyThrows
        public Text eval() {
            final CharSource tplSource = source.asCharSource(StandardCharsets.UTF_8);
            try (Reader tpl = tplSource.openStream()) {
                final StringWriter str = new StringWriter();
                try (Writer dest = new PrintWriter(str)) {
                    com.github.mustachejava.Mustache mustache = MF.compile(tpl, "template.mustache");
                    mustache.execute(dest, bean);
                }

                return new TextOf(new InputOf(str.toString()));
            }
        }
    }


    /**
     * Apply Xembler directives.
     */
    @AllArgsConstructor
    class Xembled implements Template {

        /**
         * Wrap template test output.
         * @param tpl template
         * @param project project
         */
        public Xembled(Template tpl, Project.ProjectView project) {
            this(new Text() {
                @Override
                public String asString() throws IOException {
                    return tpl.eval().asString();
                }

                @Override
                public int compareTo(Text o) {
                    return new UncheckedText(this).compareTo(o);
                }
            }, project);
        }

        /**
         * XML
         */
        private final Text xml;

        /**
         * Project props.
         */
        private final Project.ProjectView project;


        @SneakyThrows
        @Override
        public Text eval() {
            XML xml = new XMLDocument(new InputOf(this.xml).stream());
            final Directives dirs = new Directives()
                    .xpath("/project")
                    .addIf("dependencies");

            final Directives depsDirs = dirs
                    .xpath("/project/dependencies");

            project.deps().forEach(dep -> {
                depsDirs.add("dependency")
                        .comment("source-of: " + dep.source() + " ")
                        .add(ImmutableMap.of(
                                "groupId", dep.groupId(),
                                "artifactId", dep.artifactId(),
                                "version", dep.version(),
                                "scope", dep.scope()
                        ))
                        .up();
            });

            if (project.parent() != null) {
                if (!xml.nodes("/project/parent").isEmpty()) {
                    dirs.xpath("/project/parent")
                            .addIf("relativePath")
                            .set(project.parent());
                }
            }

            final Node node = new Xembler(new DirectivesNs(dirs)).apply(xml.node());
            return asString(new XMLDocument(node));
        }

        @SneakyThrows
        private Text asString(XML xml) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 4);
            Transformer transformer = transformerFactory.newTransformer(); // An identity transformer
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty("{http://xml.customer.org/xslt}indent-amount", "4");
            final StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(xml.node()), new StreamResult(writer));
            return new TextOf(writer.toString());
        }
    }

//    @AllArgsConstructor
//    class Thymeleaf implements Template {
//
//        /**
//         * Builder factory.
//         */
//        private static final TemplateEngine ENGINE = new TemplateEngine();
//
//        static {
//            ENGINE.setTemplateResolver(new StringTemplateResolver());
//        }
//
//        private final CharSource source;
//
//
//        @SneakyThrows
//        @Override
//        public Text eval() {
//            return new TextOf(ENGINE.process(
//                    source.read(),
//                    new Context()
//            ));
//        }
//    }

}
