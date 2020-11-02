package tools.jvm.mvn;

import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.cactoos.Bytes;
import org.cactoos.Text;
import org.cactoos.io.BytesOf;
import org.w3c.dom.Node;
import org.xembly.Directive;
import org.xembly.Directives;
import org.xembly.Xembler;

import javax.xml.namespace.NamespaceContext;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("NullableProblems")
public abstract class Pom {

    /**
     * Namespace.
     */
    public static final String NS = "pom";

    /**
     * Namespace xmlns.
     */
    public static final String NS_URL = "http://maven.apache.org/POM/4.0.0";
    /**
     * Xembler.
     */
    public static final String ENABLE_XEMBLY = "xembler:on";

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


    public static Pom create(Path s) {
        return new Standard(s);
    }

    public static Pom create(Text s) {
        return new Standard(s);
    }

    public static Pom create(String s) {
        return new Standard(s);
    }

    /**
     * Resolved props by xpath.
     *
     * @return properties from pom file
     */
    public Props props() {
        XML xml = xml();
        final List<java.lang.String> namespaces = getNamespaces(xml);
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

    private static List<String> getNamespaces(XML xml) {
        return xml.xpath("/*/namespace::*[name()='']");
    }

    @Override
    @SneakyThrows
    public String toString() {
        return asString(this.xml());
    }

    /**
     * XML as bytes
     * @return stream
     */
    public Bytes bytes() {
        return new BytesOf(new Text(){
            @Override
            public String asString() {
                return Pom.asString(xml());
            }

            @Override
            public int compareTo(Text o) {
                return 0;
            }
        });
    }

    private static String asString(XML xml) {
        return asString(new DOMSource(xml.node()));
    }


    @SneakyThrows
    private static String asString(Source src)  {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute("indent-number", 4);
        Transformer transformer = transformerFactory.newTransformer(); // An identity transformer
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.STANDALONE, "no");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.setOutputProperty("{http://xml.customer.org/xslt}indent-amount", "4");
        final StringWriter writer = new StringWriter();
        transformer.transform(src, new StreamResult(writer));
        return writer.toString();
    }

    /**
     * Cached xml value.
     */
    @SuppressWarnings({"Guava", "unused"})
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
     * Pom from input.
     */
    @AllArgsConstructor
    @SuppressWarnings("unused")
    private static class Standard extends Pom {

        /**
         * Ctor.
         * @param s path
         */
        public Standard(Path s) {
            this(new org.cactoos.io.InputOf(s));
        }

        /**
         * String
         * @param s str
         */
        public Standard(String s) {
            this(new org.cactoos.io.InputOf(s));
        }


        /**
         * String
         * @param s str
         */
        public Standard(Text s) {
            this(new org.cactoos.io.InputOf(s));
        }

        /**
         * Input.
         */
        private final org.cactoos.Input input;


        @SneakyThrows
        @Override
        public XML xml() {
            try (InputStream src = input.stream()) {
                return new XMLDocument(src).registerNs(NS, NS_URL);
            }
        }
    }


    public Pom xemblerd(Project project) {
        XML xml = this.xml();
        PomXPath query = new PomXPath(xml);
        xml = new XMLSanitized(xml, query);

        final List<String> comments = xml.nodes("/project/comment()").stream()
                .map(XML::toString).collect(Collectors.toList());
        for (String comment : comments) {
            final int enable = comment.indexOf(ENABLE_XEMBLY);
            if (enable != -1) {
                int i = enable + ENABLE_XEMBLY.length();
                xml = transform(xml, project, comment.substring(i), query);
            }
        }
        XML finalXml = xml;
        return new Cached(new Pom() {
            @Override
            public XML xml() {
                return finalXml;
            }
        });
    }

    /**
     * Build xml scalar based on comment flags
     * @param xml xml
     * @param flagsLine flags
     * @return scalar
     */
    @SneakyThrows
    @SuppressWarnings("StaticPseudoFunctionalStyleMethod")
    private static XML transform(XML xml, Project project, String flagsLine, PomXPath query) {
        final List<XeFunc> chain = Lists.newArrayList(
                new DefaultStrucTags(),
                new ClearDeps(),
                new AddProjectDeps()
        );
        for (String trashSign : new String[]{"-->", "\n"}) {
            flagsLine = flagsLine.replace(trashSign, "");
        }
        final String finalFlagsLine = flagsLine;
        Stream.of(flagsLine.split(" ")).map(String::trim).filter(s -> !s.isEmpty()).forEach(flag -> {
            //noinspection SwitchStatementWithTooFewBranches
            switch (flag.trim()) {
                case "no-drop-deps-first":
                    chain.removeIf(x -> x instanceof ClearDeps);
                    break;
                default:
                    throw new IllegalStateException("unknown: " + flag + " - " + finalFlagsLine);
            }
        });

        @SuppressWarnings("ConstantConditions")
        final Iterable<Directive> dirs = Iterables.concat(Iterables.transform(chain, dir -> dir.apply(project, xml)));
        final Node node = new Xembler(new XemblerDirs(dirs, query)).apply(xml.node());
        return new XMLDocument(node);
    }

    /**
     * Single directives resolver.
     */
    interface XeFunc {
        Iterable<Directive> apply(Project project, XML xml);
    }

    static class ClearDeps implements XeFunc {

        /**
         * Tag to indicate that dependency is freeze.
         */
        public static final String DO_NOT_RM_TAG = "no-drop-dep";


        @Override
        public Iterable<Directive> apply(Project project, XML xml) {
            return new Directives()
                    .xpath("/project/dependencies/dependency[not(@xe:class=\"" + DO_NOT_RM_TAG + "\")]")
                    .remove();
        }
    }

    static class DefaultStrucTags implements XeFunc {

        @Override
        public Iterable<Directive> apply(Project project, XML xml) {
            final Directives directives = new Directives()
                    .xpath("/project")
                    .addIf("dependencies");
            final Project.ProjectView projectView = project.toView();
            if (!Strings.isNullOrEmpty(projectView.parent())) {
                if (!xml.nodes("/project/parent").isEmpty()) {
                    directives.xpath("/project/parent")
                            .addIf("relativePath")
                            .set(projectView.parent());
                }
            }
            return directives;
        }
    }

    static class AddProjectDeps implements XeFunc {


        @Override
        public Iterable<Directive> apply(Project project, XML xml) {
            final Directives directives = new Directives();
            directives.xpath("/project/dependencies");
            project.deps().forEach(dep -> {
                directives.add("dependency")
                        .comment("source-of: " + dep.source() + " ")
                        .add(ImmutableMap.of(
                                "groupId", dep.groupId(),
                                "artifactId", dep.artifactId(),
                                "version", dep.version(),
                                "scope", dep.scope()
                        ))
                        .up();
            });
            return directives;
        }
    }

    /**
     * Pom sanitized xml.
     */
    private static class XMLSanitized implements XML {

        /**
         * Query transform.
         */
        private final PomXPath map;

        /**
         * Origin.
         */
        private final XML xml;

        /**
         * Ctor.
         * @param xml xml
         */
        private XMLSanitized(XML xml, PomXPath query) {
            this.xml = xml;
            this.map = query ;
        }

        @Override
        public List<String> xpath(String query) {
            return xml.xpath(map.apply(query));
        }

        @Override
        public List<XML> nodes(String query) {
            return xml.nodes(map.apply(query));
        }

        @Override
        public XML registerNs(String prefix, Object uri) {
            return xml.registerNs(prefix, uri);
        }

        @Override
        public XML merge(NamespaceContext context) {
            return xml.merge(context);
        }

        @Override
        public Node node() {
            return xml.node();
        }

        @Override
        public String toString() {
            return xml.toString();
        }
    }
}
