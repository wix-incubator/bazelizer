package tools.jvm.mvn;

import com.google.common.base.*;
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

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("NullableProblems")
public abstract class Pom {

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
            public String asString() throws IOException {
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
    static class FromInput extends Pom {

        /**
         * Ctor.
         * @param s path
         */
        public FromInput(Path s) {
            this(new org.cactoos.io.InputOf(s));
        }

        /**
         * String
         * @param s str
         */
        public FromInput(String s) {
            this(new org.cactoos.io.InputOf(s));
        }


        /**
         * String
         * @param s str
         */
        public FromInput(Text s) {
            this(new org.cactoos.io.InputOf(s));
        }

        private final org.cactoos.Input input;


        @SneakyThrows
        @Override
        public XML xml() {
            try (InputStream src = input.stream()) {
                return new XMLDocument(src)
                        .registerNs("pom", "http://maven.apache.org/POM/4.0.0");
            }
        }
    }


    public Pom xemblerd(Project project) {
        XML xml = this.xml();
        final List<String> comments = xml.nodes("/project/comment()")
                .stream().map(XML::toString).collect(Collectors.toList());
        for (String comment : comments) {
            final int enable = comment.indexOf(ENABLE_XEMBLY);
            if (enable != -1) {
                int i = enable + ENABLE_XEMBLY.length();
                final String[] tagsLine = toTags(comment.substring(i));
                xml = apply(xml, project, tagsLine);
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

    public static String[] toTags(String str) {
        final String replace = str
                .replace("-->", "")
                .replace("\n", "");
        if (replace.isEmpty()) return new String[] {};
        return replace.split(" ");
    }



    /**
     * Build xml scalar based on comment flags
     * @param xml xml
     * @param flags flags
     * @return scalar
     */
    @SneakyThrows
    @SuppressWarnings("StaticPseudoFunctionalStyleMethod")
    private static XML apply(XML xml, Project project, String[] flags) {
        final List<XeFunc> dirs = Lists.newArrayList(
                new DefaultStrucTags(),
                new ClearDeps(),
                new AddDepsTags()
        );

        for (String flag : flags) {
            //noinspection SwitchStatementWithTooFewBranches
            switch (flag.trim()) {
                case "no-drop-deps-first":
                    dirs.removeIf(x -> x instanceof ClearDeps);
                    break;
                default:
                    throw new IllegalStateException("unknown: " + flag + ". " + Arrays.toString(flags));
            }
        }

        final Node node = new Xembler(
                new XemblerNs(Iterables.concat(
                        Iterables.transform(dirs, dir -> dir.apply(project,xml)))
                ).registerNs("xe", "http://www.w3.org/2000/svg")
        ).apply(xml.node());

        return new XMLDocument(asString(new DOMSource(node)));
    }

    /**
     * Single directives resolver.
     */
    interface XeFunc {
        Iterable<Directive> apply(Project project, XML xml);
    }


    static class ClearDeps implements XeFunc {
        @Override
        public Iterable<Directive> apply(Project project, XML xml) {
            return new Directives()
                    .xpath("/project/dependencies/dependency[not(@xe:class=\"no-drop-dep\")]")
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

    static class AddDepsTags implements XeFunc {
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

}
