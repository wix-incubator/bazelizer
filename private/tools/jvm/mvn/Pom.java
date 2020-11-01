package tools.jvm.mvn;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.cactoos.Bytes;
import org.cactoos.Input;
import org.cactoos.Text;
import org.cactoos.io.BytesOf;
import org.cactoos.io.InputOf;
import org.cactoos.iterable.Joined;
import org.cactoos.iterable.Mapped;
import org.cactoos.text.TextOf;
import org.w3c.dom.Node;
import org.xembly.Directive;
import org.xembly.Directives;
import org.xembly.Xembler;
import picocli.CommandLine;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

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
    static class AsInput extends Pom {

        /**
         * Ctor.
         * @param s path
         */
        public AsInput(Path s) {
            this(new org.cactoos.io.InputOf(s));
        }

        /**
         * String
         * @param s str
         */
        public AsInput(String s) {
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


    public static Pom xembledBy(org.cactoos.Input in, Project project) {
        XML xml = new AsInput(in).xml();
        final List<String> comments = xml.nodes("/project/comment()")
                .stream().map(XML::toString).map(Pom::clear).collect(Collectors.toList());
        for (String comment : comments) {
            final int enable = comment.indexOf(ENABLE_XEMBLY);
            if (enable != -1) {
                int i = enable + ENABLE_XEMBLY.length();
                final String[] cmd = comment.substring(i).split(" ");
                xml = resolve(xml, cmd).transformBy(project);
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

    public static String clear(String str) {
        return str.replace("-->", "").replace("\n", "");
    }


    /**
     * Xml scalara with transformation based on Project state
     */
    interface XeFuncs {
        XML transformBy(Project project);
    }


    /**
     * Build xml scalar based on comment flags
     * @param xml xml
     * @param flags flags
     * @return scalar
     */
    private static XeFuncs resolve(XML xml, String[] flags) {
        final XeFuncsFlags params;
        try {
            params = new XeFuncsFlags(xml);
            final CommandLine.ParseResult result = new CommandLine(params).parseArgs(flags);
            Preconditions.checkState(!result.isUsageHelpRequested());
            Preconditions.checkState(!result.isVersionHelpRequested());
        } catch (CommandLine.ParameterException e) {
            throw new ToolException("[pom flags] flags " + Arrays.toString(flags)   + " not valid: "
                    + e.getMessage() + "\n correct flags: "
                    + e.getCommandLine().getHelp().synopsis(0)
            );
        }
        return params;
    }

    /**
     * Scalar
     */
    @CommandLine.Command(name = ENABLE_XEMBLY)
    public static class XeFuncsFlags implements XeFuncs {
        @CommandLine.Option(names = {"-fcd", "--first-clear-deps"}, defaultValue = "true")
        public boolean clearDepsFirst = true;

        private final XML xml;

        /**
         * Ctor.
         * @param xml pom xml
         */
        public XeFuncsFlags(XML xml) {
            this.xml = xml;
        }

        @SneakyThrows
        public XML transformBy(Project project) {
            final ArrayList<XeFunc> chain = Lists.newArrayList(
                    new DefaultStrucTags(),
                    this.clearDepsFirst ? new ClearDepr() : NOP,
                    new DepsTags()
            );

            final Node node = new Xembler(
                    new XemblerNs(
                            new Joined<>(
                                    new Mapped<>(
                                            chain,
                                            func -> func.get(project, xml)
                                    )
                            )
                    )
            ).apply(xml.node());

            return new XMLDocument(
                    asString(new DOMSource(node))
            );
        }
    }

    /**
     * Single directives resolver.
     */
    interface XeFunc {
        Iterable<Directive> get(Project project, XML xml);
    }


    static final XeFunc NOP = (project, xml) -> ImmutableList.of();


    static class ClearDepr implements XeFunc {
        @Override
        public Iterable<Directive> get(Project project, XML xml) {
            return new Directives()
                    .xpath("/project/dependencies/dependency")
                    .remove();
        }
    }

    static class DefaultStrucTags implements XeFunc {

        @Override
        public Iterable<Directive> get(Project project, XML xml) {
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

    static class DepsTags implements XeFunc {
        @Override
        public Iterable<Directive> get(Project project, XML xml) {
            final Directives directives = new Directives();
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
