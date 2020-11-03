package tools.jvm.mvn;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.jcabi.xml.XPathContext;
import lombok.experimental.Delegate;
import org.cactoos.Scalar;
import org.w3c.dom.Node;
import org.xembly.Directive;
import org.xembly.Directives;
import org.xembly.SyntaxException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;

public class XemblerAugment {

    /**
     * Execute within xpath context.
     * @param context context
     * @param block block
     * @return node
     * @throws Exception if any
     */
    public static Node withinContenxt(XPathContext context, Scalar<Node> block) throws Exception {
        final XPathFactory real = FACTORY.get();
        if (!(real instanceof XPathFactoryWrap)) {
            FACTORY.set(new XPathFactoryWrap(real, context));
        }
        try {
            return block.value();
        } finally {
            FACTORY.set(real);
        }
    }

    /**
     * XPath Query.
     */
    public interface XPathQuery extends Function<String,String> {}

    /**
     * Directives with xpath context.
     */
    public static class XPathContextOf implements Iterable<Directive> {

        /**
         * Extract brakes data.
         */
        public static final Pattern BRAKETS = Pattern.compile("\"(.*?)\"");

        /**
         * XPath query verb keyword.
         */
        public static final String XPATH = "XPATH";

        /**
         * XPath query verb.
         */
        public static final String XPATH_VERB = "XPATH \"%s\"";

        /**
         * Origin.
         */
        @Delegate
        private final Iterable<Directive> dirs;

        /**
         * Ctor.
         * @param func xpath query func
         * @param dirs directives
         */
        public XPathContextOf(XPathQuery func, Iterable<Directive> dirs) {
            List<String> verbs = newArrayList(transform(dirs, Object::toString));
            try {
                this.dirs = new Directives(
                        verbs.stream().map(verb -> {
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

        @Override
        public String toString() {
            return "XemblyFunc.AllOf("+ Iterables.toString(dirs)+")";
        }
    }

    private static class XPathFactoryWrap extends XPathFactory {

        /**
         * Origin.
         */
        @Delegate(excludes = {WithNewXPath.class})
        private final XPathFactory forward;

        /**
         * XPath context.
         */
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

        @SuppressWarnings("UnnecessaryInterfaceModifier")
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
            e.printStackTrace();
        }
        FACTORY = f;
    }
    /**
     * XpathDirective factory ref.
     */
    private final static ThreadLocal<XPathFactory> FACTORY;

}
