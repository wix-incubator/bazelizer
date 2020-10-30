package tools.jvm.mvn;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.cactoos.Text;
import org.cactoos.io.InputOf;
import org.cactoos.text.TextOf;
import org.w3c.dom.Node;
import org.xembly.Directives;
import org.xembly.Xembler;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public interface PomXml {

    /**
     * Transform xml
     * @param source source xml
     * @return xml
     */
    Text apply(Text source);



    @AllArgsConstructor
    class Xemblerd implements PomXml {

        private final Project project;

        @SneakyThrows
        @Override
        public Text apply(Text source) {
            XML xml = new XMLDocument(new InputOf(source).stream());

            final Directives dirs = new Directives()
                    .xpath("/project")
                    .addIf("dependencies");
//                    .xpath("/project/dependencies/dependency")
//                    .remove();

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

            if (project.parentPom() != null) {
                if (!xml.nodes("/project/parent").isEmpty()) {
                    dirs.xpath("/project/parent")
                            .addIf("relativePath")
                            .set(project.parentPom().toString());
                }
            }

            final Node node = new Xembler(
                    new DirectivesNs(dirs)
            ).apply(xml.node());

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
}
