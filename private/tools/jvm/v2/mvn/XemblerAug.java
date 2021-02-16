package tools.jvm.v2.mvn;

import com.google.common.base.Preconditions;
import com.google.common.collect.Streams;
import com.jcabi.xml.XML;
import com.jcabi.xml.XPathContext;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;
import lombok.experimental.UtilityClass;
import org.cactoos.Scalar;
import org.w3c.dom.Node;
import org.xembly.Directive;
import org.xembly.Directives;
import org.xembly.SyntaxException;
import org.xembly.Xembler;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@UtilityClass
public class XemblerAug {

    @SneakyThrows
    public static XML applyQuietly(XML xml, XPathContext context, Iterable<Directive> dirs) {
        final XemblerXML input = new XemblerXML(xml);
        final Iterable<Directive> directives = updateXPathTypo(input.getXpathTypo(), dirs);
        final Node dom = withinContext(context,
                () -> new Xembler(directives).applyQuietly(input.node())
        );
        return new XemblerXML(dom, input.getXpathTypo());
    }

    /**
     * Execute within xpath context.
     * @param context context
     * @param block block
     * @return node
     * @throws Exception if any
     */
    private static Node withinContext(XPathContext context, Scalar<Node> block) throws Exception {
        final XPathFactory real = _org_xembly_XpathDirectiveFactory.get();
        if (!(real instanceof XPathFactoryWrap)) {
            _org_xembly_XpathDirectiveFactory.set(new XPathFactoryWrap(real, context));
        }
        try {
            return block.value();
        } finally {
            _org_xembly_XpathDirectiveFactory.set(real);
        }
    }


    @SuppressWarnings("UnstableApiUsage")
    private static Iterable<Directive> updateXPathTypo(XPathTypo func, Iterable<Directive> dirs) {
        try {
            return new Directives(
                    Streams.stream(dirs).map(dir -> {
                        final String verb = dir.toString();
                        // this is actually a bug how comments are represented.
                        if (verb.contains("CDATA") && dir.getClass() == _org_xembly_CommentDirective_Type) {
                            final Matcher matcher = BRAKETS.matcher(verb);
                            Preconditions.checkState(matcher.find(), "verb %s invalid", verb);
                            return String.format(COMMENT_VERB, matcher.group(1));
                        }
                        if (verb.contains(XPATH)) {
                            final Matcher matcher = BRAKETS.matcher(verb);
                            Preconditions.checkState(matcher.find(), "verb %s invalid", verb);
                            return String.format(XPATH_VERB, func.apply(matcher.group(1)));
                        }
                        return verb;
                    }).collect(Collectors.joining(";", "", ";"))
            );
        } catch (SyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class XPathFactoryWrap extends XPathFactory {
        @Delegate(excludes = {WithNewXPath.class})
        private final XPathFactory forward;
        private final XPathContext context;

        /**
         * Ctor.
         * @param forward origin
         * @param context ctx
         */
        private XPathFactoryWrap(XPathFactory forward, XPathContext context) {
            this.forward = forward;
            this.context = context;
        }

        @Override
        public XPath newXPath() {
            final XPath xPath = forward.newXPath();
            xPath.setNamespaceContext(context);
            return xPath;
        }

        @SuppressWarnings({"UnnecessaryInterfaceModifier", "unused"})
        public static interface WithNewXPath {
            public abstract XPath newXPath();
        }
    }

    static {
        ThreadLocal<XPathFactory> f = null;
        try {
            final Class<?> clz = Class.forName("org.xembly.XpathDirective");
            @SuppressWarnings("JavaReflectionMemberAccess")
            final Field factory = clz.getDeclaredField("FACTORY");
            factory.setAccessible(true);
            //noinspection unchecked
            f = (ThreadLocal<XPathFactory>) factory.get(clz);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("org.xembly", e);
        }
        _org_xembly_XpathDirectiveFactory = f;

        Class<?> cls;
        try {
            cls = Class.forName("org.xembly.CommentDirective");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("org.xembly", e);
        }
        _org_xembly_CommentDirective_Type = cls;
    }


    private static final Class<?> _org_xembly_CommentDirective_Type;
    private final static ThreadLocal<XPathFactory> _org_xembly_XpathDirectiveFactory;

    public static final Pattern BRAKETS = Pattern.compile("\"(.*?)\"");
    public static final String XPATH = "XPATH";
    public static final String XPATH_VERB = "XPATH \"%s\"";
    public static final String COMMENT_VERB = "COMMENT \"%s\"";
}
