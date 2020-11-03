package tools.jvm.mvn;

import com.google.common.collect.ImmutableList;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import com.jcabi.xml.XPathContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.cactoos.Input;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xembly.Directive;
import org.xembly.Xembler;

import java.io.InputStream;
import java.util.List;
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
    class PomXempled implements Pom {

        /**
         * Ctor.
         * @param input input
         * @param project project
         */
        public PomXempled(Input input,Project project) {
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
                new XemblyFunc.PomDropDeps()
        );

        @Override
        public XML xml() throws Exception {
            final XML origXML = pom.xml();
            final SanitizedXML xml = new SanitizedXML(
                    origXML
            );

            final Iterable<Directive> dirs = new XemblerAugment.XPathContextOf(
              xml.getXpathQuery(), concat(transform(xe, d -> d.dirs(project, xml)))
            );
            final Node node = XemblerAugment.withinContenxt(XPATH_CONTEXT, () ->
                    new Xembler(dirs).apply(
                        xml.node()
                    )
            );
            trimWhitespace(node);
            return new XMLDocument(node);
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

            /**
             * Ctor.
             * @param orig original xml
             */
            private SanitizedXML(XML orig) {
                this.orig = orig;
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
        }
    }



}
