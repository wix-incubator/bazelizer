package tools.jvm.mvn;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.ByteSource;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import com.jcabi.xml.XPathContext;
import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;
import org.cactoos.Input;
import org.cactoos.Scalar;
import org.cactoos.Text;
import org.cactoos.scalar.UncheckedScalar;
import org.cactoos.text.UncheckedText;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xembly.Directive;
import org.xembly.Xembler;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;

public interface Pom {

    String NAMESPACE_XPATH = "/*/namespace::*[name()='']";
    String POM_NS_URI = "http://maven.apache.org/POM/4.0.0";
    String POM_NS = "pom";

    /**
     * Global xpath context.
     */
    XPathContext XPATH_CONTEXT = new XPathContext()
            .add(POM_NS, POM_NS_URI)
            .add("xe", "http://www.w3.org/1999/xhtml");

    /**
     * Pom as XMP
     * @return xml
     */
    XML xml();

    /**
     * Group id
     * @return str
     */
    default String groupId() {
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
    default String artifactId() {
        return xml().xpath("/project/artifactId/text()").get(0);
    }

    /**
     * Version
     * @return str
     */
    default String version() {
        return xml().xpath("/project/version/text()").get(0);
    }

    /**
     * Loaded pom xml.
     */
    @AllArgsConstructor
    class Standard implements Pom {

        /**
         * Ctor.
         * @param src byte source
         */
        public Standard(ByteSource src) {
            this(src::openStream);
        }

        /**
         * XML.
         */
        private final Input xml;


        @Override
        @SneakyThrows
        public XML xml() {
            try (InputStream src = xml.stream()) {
                return new SanitizedXML(
                        new XMLDocument(src).merge(XPATH_CONTEXT)
                );
            }
        }

        @Override
        public String toString() {
            return xml.toString();
        }
    }

    @SuppressWarnings("Guava")
    class Cached implements Pom {

        public Cached(Pom pom) {
            this((Supplier<XML>) pom::xml);
        }

        @SuppressWarnings("unused")
        public Cached(Scalar<XML> xml) {
            this((Supplier<XML>) () -> new UncheckedScalar<>(xml).value());
        }

        private Cached(Supplier<XML> xml) {
            this.xml = Suppliers.memoize(xml);
        }

        /**
         * XML.
         */
        private final Supplier<XML> xml;

        @Override
        public XML xml() {
            return xml.get();
        }

        @Override
        public String toString() {
            return xml().toString();
        }
    }

    /**
     * POM xml with specific transformations like xslt.
     */
    @SuppressWarnings({"StaticPseudoFunctionalStyleMethod","ConstantConditions"})
    @AllArgsConstructor
    class PomXembly implements Pom {

        /**
         * Transformer factory.
         */
        private static final TransformerFactory TFACTORY =
                TransformerFactory.newInstance();

        @AllArgsConstructor
        enum XemblyFeature {
            ENABLED("xembly:on"),
            NO_DROP_DEPS("xembly:no-drop-deps");

            private final String qname;
        }

        /**
         * Ctor.
         * @param input input
         * @param project project
         */
        public PomXembly(Input input, Project project) {
            this(new Standard(input), project);
        }

        /**
         * Pom.
         */
        private final Pom pom;

        /**
         * Project.
         */
        private final Project project;

        /**
         * Xembly directives.
         */
        private final List<XemblyFunc> xe = ImmutableList.of(
                new XemblyFunc.PomDefaultStruc(),
                new XemblyFunc.PomParentRelPath(),
                new XemblyFunc.PomDropDeps(),
                new XemblyFunc.AppendDeps()
        );

        @SneakyThrows
        @Override
        public XML xml()  {
            final XML origXML = pom.xml();
            final SanitizedXML xml = new SanitizedXML(origXML);
            final Set<XemblyFeature> features = getFeatures(xml);
            if (!features.contains(XemblyFeature.ENABLED)) {
                return xml;
            }
            final List<XemblyFunc> funcs = Lists.newArrayList(xe);
            if (features.contains(XemblyFeature.NO_DROP_DEPS)) {
                funcs.removeIf(func -> func instanceof XemblyFunc.PomDropDeps);
            }

            final Iterable<Directive> dirs = new XemblerAugment.AugmentedDirs(
              xml.getXpathQuery(), concat(transform(funcs, d -> d.dirs(project, xml)))
            );
            final Node node = new XemblerAugment(
                    new Xembler(dirs),
                    XPATH_CONTEXT
            ).applyQuietly(xml.node());

            return new SanitizedXML(node);
        }


        /**
         * Xemlby features.
         * @return features
         */
        @SuppressWarnings("StaticPseudoFunctionalStyleMethod")
        private Set<PomXembly.XemblyFeature> getFeatures(XML xml) {
            Set<PomXembly.XemblyFeature> ll = Sets.newHashSet();
            final List<String> xpath1 = Lists.transform(xml.nodes("/project/comment()"), Objects::toString);
            final List<String> xpath2 = Lists.transform(xml.nodes("/comment()"), Objects::toString);
            concat(xpath1, xpath2).forEach(comment -> {
                for (PomXembly.XemblyFeature value : PomXembly.XemblyFeature.values()) {
                    if (comment.contains(value.qname)) {
                        ll.add(value);
                    }
                }
            });
            return ll;
        }
    }

    /**
     * Sanitized XML with auto prefixing xpath queries.
     */
    class SanitizedXML implements XML {

        /**
         * XML.
         */
        @Delegate(excludes = {WithXPath.class})
        private final XML origin;

        /**
         * XPath Query mapper
         */
        @Getter
        private final XemblerAugment.XPathQuery xpathQuery;


        @SuppressWarnings("Guava")
        private final Supplier<String> xml;

        /**
         * Ctor
         * @param node original node
         */
        public SanitizedXML(Node node) {
            this(new XMLDocument(node).merge(Pom.XPATH_CONTEXT));
        }

        /**
         * Ctor.
         * @param input original xml
         */
        @SuppressWarnings("Guava")
        public SanitizedXML(XML input) {
            XML xml = input;
            XemblerAugment.XPathQuery xPathQuery = null;
            Supplier<String> asString = Suppliers.memoize(
                    () -> new PrettyPrintXml(input).asString()
            );

            if (xml instanceof SanitizedXML) {
                xml = ((SanitizedXML) input).origin;
                xPathQuery = ((SanitizedXML) input).xpathQuery;
                asString = ((SanitizedXML) input).xml;
            }

            this.origin = xml;
            this.xpathQuery = xPathQuery != null ? xPathQuery : newXPathQuery(xml);
            this.xml = asString;
        }


        private static XemblerAugment.XPathQuery newXPathQuery(XML orig) {
            XemblerAugment.XPathQuery queryMap = s -> s;
            final List<String> namespaces = orig.xpath(NAMESPACE_XPATH);
            if (namespaces.contains(POM_NS_URI)) {
                queryMap = new XemblerAugment.XPathQueryPref(POM_NS);
            }
            return queryMap;
        }

        @Override
        public List<String> xpath(String query) {
            return origin.xpath(xpathQuery.apply(query));
        }

        @Override
        public List<XML> nodes(String query) {
            return origin.nodes(xpathQuery.apply(query));
        }

        @SuppressWarnings("unused")
        public interface WithXPath {
            List<String> xpath(String query);
            List<XML> nodes(String query);
        }

        @Override
        public String toString() {
            return this.xml.get();
        }
    }

    @AllArgsConstructor
    class PrettyPrintXml implements Text {

        private final XML input;

        @Override
        public String asString() {
            final Node node = input.node();
            trimWhitespace(node);
            return prettyPrint(node);
        }

        @Override
        public int compareTo(@SuppressWarnings("NullableProblems") Text t) {
            return new UncheckedText(t).compareTo(this);
        }


        private static void trimWhitespace(Node node) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); ++i) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
                    System.out.println("");
                }
                if (child.getNodeType() == Node.TEXT_NODE) {
                    child.setTextContent(child.getTextContent().trim());
                }
                trimWhitespace(child);
            }
        }

        private static String prettyPrint(Node node) {
            final StringWriter writer = new StringWriter();
            try {
                final Transformer trans = PomXembly.TFACTORY.newTransformer();
                trans.setOutputProperty(OutputKeys.INDENT, "yes");
                trans.setOutputProperty(OutputKeys.VERSION, "1.0");
                trans.setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "2");
                if (!(node instanceof Document)) {
                    trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                }
                trans.transform(new DOMSource(node), new StreamResult(writer));
            } catch (TransformerException ex) {
                throw new IllegalStateException(ex);
            }
            return writer.toString();
        }
    }

}
