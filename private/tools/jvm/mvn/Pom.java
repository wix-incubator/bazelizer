package tools.jvm.mvn;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import com.jcabi.xml.XPathContext;
import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.cactoos.Input;
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
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;

public interface Pom {

    String NAMESPACE_XPATH = "/*/namespace::*[name()='']";
    String POM_URI = "http://maven.apache.org/POM/4.0.0";
    String POM = "pom";

    /**
     * Global xpath context.
     */
    XPathContext XPATH_CONTEXT = new XPathContext()
            .add(POM, POM_URI)
            .add("xe", "http://www.w3.org/1999/xhtml");

    @Data
    class Props {
        final String groupId;
        final String artifactId;
        final String version;
    }

    /**
     * Pom as XMP
     * @return xml
     * @throws Exception if any error
     */
    XML xml() throws Exception ;



    @AllArgsConstructor
    class Standard implements Pom {

        private final Input data;

        @Override
        public XML xml() throws Exception {
            try (InputStream src = data.stream()) {
                return new XMLDocument(src).merge(XPATH_CONTEXT);
            }
        }

        public Props props() throws Exception {
            XML xml = xml();
            final List<String> namespaces = xml.xpath(NAMESPACE_XPATH);
            if (!namespaces.isEmpty()) {
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

        @Override
        public XML xml() throws Exception {
            final XML origXML = pom.xml();
            final SanitizedXML xml = new SanitizedXML(
                    origXML
            );
            final Set<XemblyFeature> features = xml.getFeatures();
            if (!features.contains(XemblyFeature.ENABLED)) {
                return xml;
            }

            final List<XemblyFunc> funcs = Lists.newArrayList(xe);
            if (features.contains(XemblyFeature.NO_DROP_DEPS)) {
                funcs.removeIf(func -> func instanceof XemblyFunc.PomDropDeps);
            }

            final Iterable<Directive> dirs = new XemblerAugment.XPathContextOf(
              xml.getXpathQuery(), concat(transform(funcs, d -> d.dirs(project, xml)))
            );
            final Node node = XemblerAugment.withinContenxt(XPATH_CONTEXT, () ->
                    new Xembler(dirs).apply(
                        xml.node()
                    )
            );
            trimWhitespace(node);
            return new SanitizedXML(node);
        }

        private static void trimWhitespace(Node node) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); ++i) {
                Node child = children.item(i);
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

        /**
         * Sanitized XML with auto prefixing xpath queries.
         */
        private static class SanitizedXML implements XML {

            /**
             * XML.
             */
            @Delegate(excludes = {WithXPath.class})
            private final XML orig;

            /**
             * XPath Query mapper
             */
            @Getter
            private final XemblerAugment.XPathQuery xpathQuery;


            private final Supplier<String> xml;

            /**
             * Xemlby features.
             * @return features
             */
            public Set<XemblyFeature> getFeatures() {
                Set<XemblyFeature> ll = Sets.newHashSet();
                final List<String> xpath1 = Lists.transform(this.nodes("/project/comment()"), Objects::toString);
                final List<String> xpath2 = Lists.transform(this.nodes("/comment()"), Objects::toString);
                concat(xpath1, xpath2).forEach(comment -> {
                    for (XemblyFeature value : XemblyFeature.values()) {
                        if (comment.contains(value.qname)) {
                            ll.add(value);
                        }
                    }
                });
                return ll;
            }

            /**
             * Ctor
             * @param node original node
             */
            private SanitizedXML(Node node) {
                this(new XMLDocument(node));
            }

            /**
             * Ctor.
             * @param orig original xml
             */
            private SanitizedXML(XML orig) {
                this.orig = orig;
                this.xml = Suppliers.memoize(() -> {
                    final Node node = orig.node();
                    trimWhitespace(node);
                    return prettyPrint(node);
                });
                XemblerAugment.XPathQuery queryMap = s -> s;
                if (orig.xpath(NAMESPACE_XPATH).contains(POM_URI)) {
                    queryMap = query -> Stream.of(query.split("/"))
                            .filter(t -> !t.isEmpty() && !t.contains("()"))
                            .map(t -> POM + ":" + t)
                            .collect(Collectors.joining("/", "/", ""));
                }
                this.xpathQuery = queryMap;

            }

            @Override
            public List<String> xpath(String query) {
                return orig.xpath(xpathQuery.apply(query));
            }

            @Override
            public List<XML> nodes(String query) {
                return orig.nodes(xpathQuery.apply(query));
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
    }



}
