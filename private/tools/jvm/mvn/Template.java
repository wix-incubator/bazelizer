package tools.jvm.mvn;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.common.collect.ImmutableMap;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.cactoos.Text;
import org.cactoos.io.InputOf;
import org.cactoos.io.InputStreamOf;
import org.cactoos.text.TextOf;
import org.w3c.dom.Node;
import org.xembly.Directives;
import org.xembly.Xembler;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

public interface Template<K> {


    /**
     * Evaluate template.
     *
     * @return char source
     */
    Text eval(Text tpl, K been);

    /**
     * Template envelope via mustache.
     */
    class Mustache<K> implements Template<K> {
        private static final MustacheFactory MF = new DefaultMustacheFactory();


        @Override
        @SneakyThrows
        public Text eval(Text input, K bean) {
            try (Reader tpl = new InputStreamReader(new InputStreamOf(input))) {
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
    class Xembled implements Template<Project.ProjectView> {


        @Override
        @SneakyThrows
        public Text eval(Text in, Project.ProjectView project) {

            XML xml = new XMLDocument(new InputOf(in).stream());
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

            final Node node = new Xembler(
                    new XemblerNs(dirs)
            ).apply(xml.node());
            return asString(new XMLDocument(node));        }


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

}
