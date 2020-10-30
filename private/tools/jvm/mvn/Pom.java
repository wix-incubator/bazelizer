package tools.jvm.mvn;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

public abstract class Pom {


    /**
     * POM xml.
     * @return xml
     */
    public abstract XML xml();

    /**
     * Properties.
     */
    @Data
    static class Props {
        final java.lang.String groupId;
        final java.lang.String artifactId;
        final java.lang.String version;
    }


    /**
     * Resolved props by xpath.
     *
     * @return properties from pom file
     */
    public Props props() {
        XML xml = xml();
        final List<java.lang.String> namespaces = xml.xpath("/*/namespace::*[name()='']");
        if (!namespaces.isEmpty()) {
            java.lang.String gid = xml.xpath("/pom:project/pom:groupId/text()").get(0);
            java.lang.String aid = xml.xpath("/pom:project/pom:artifactId/text()").get(0);
            java.lang.String v = xml.xpath("/pom:project/pom:version/text()").get(0);
            return new Props(gid, aid, v);
        } else {
            java.lang.String gid = xml.xpath("/project/groupId/text()").get(0);
            java.lang.String aid = xml.xpath("/project/artifactId/text()").get(0);
            java.lang.String v = xml.xpath("/project/version/text()").get(0);
            return new Props(gid, aid, v);
        }
    }


    @SneakyThrows
    public String toPrettyXml() {
        final XML xml = this.xml();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute("indent-number", 4);
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter stringWriter = new StringWriter();
        transformer.transform(new DOMSource(xml.node()), new StreamResult(stringWriter));
        return stringWriter.toString();
    }

    @Override
    @SneakyThrows
    public String toString() {
        return this.toPrettyXml();
    }


    @SuppressWarnings("Guava")
    private static class Cached extends Pom {

        private final Supplier<XML> pom;

        private Cached(Pom pom) {
            this.pom = Suppliers.memoize(pom::xml);
        }

        @Override
        public XML xml() {
            return pom.get();
        }
    }

    /**
     *
     */
    @AllArgsConstructor
    static class StringOf extends Pom {

        @SuppressWarnings("UnstableApiUsage")
        public StringOf(Path s) {
            this(Files.asByteSource(s.toFile()).asCharSource(StandardCharsets.UTF_8));
        }

        public StringOf(String s) {
            this(CharSource.wrap(s));
        }

        private final CharSource input;

        @SneakyThrows
        @SuppressWarnings("UnstableApiUsage")
        @Override
        public XML xml() {
            try (InputStream src = input.asByteSource(StandardCharsets.UTF_8).openStream()) {
                return new XMLDocument(src)
                        .registerNs("pom", "http://maven.apache.org/POM/4.0.0");
            }
        }
    }
}
