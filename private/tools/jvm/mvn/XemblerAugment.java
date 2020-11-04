package tools.jvm.mvn;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.jcabi.xml.XPathContext;
import lombok.ToString;
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
    public static Node withinContext(XPathContext context, Scalar<Node> block) throws Exception {
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
    public interface XPathQuery extends Function<String,String> {
    }



    @ToString
    public static class XPathQueryPref implements XPathQuery {

        @SuppressWarnings("RegExpRedundantEscape")
        private final static Pattern ATTRS = Pattern.compile("^\\w+(\\[(.*?)\\])");

        /**
         * Ctor.
         * @param prefix prefix
         */
        public XPathQueryPref(String prefix) {
            this.prefix = prefix;
            this.fmt = prefix + ":%s";
        }

        private final String prefix;

        private final String fmt;

        @Override
        public String apply(String query) {
            final List<String> tokens = Lists.newArrayList();
            String startOf = "";
            for (String token : query.split("/")) {
                if (token.isEmpty()) {
                    //noinspection StringConcatenationInLoop
                    startOf += "/";
                    continue;
                }
                if (token.contains("()")) {
                    tokens.add(token);
                    continue;
                }
                if (ATTRS.matcher(token).matches()) {
                    tokens.add(String.format(fmt, token));
                    continue;
                }
                if (!isAlpha(token)) {
                    tokens.add(token);
                    continue;
                }
                tokens.add(String.format(fmt, token));
            }
            return startOf + String.join("/", tokens);
        }

        private static boolean isAlpha(String str) {
            int sz = str.length();
            for (int i = 0; i < sz; i++) {
                if (!Character.isLetter(str.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
    }



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
            e.printStackTrace();
        }
        FACTORY = f;
    }
    /**
     * XpathDirective factory ref.
     */
    private final static ThreadLocal<XPathFactory> FACTORY;

}
