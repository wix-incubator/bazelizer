package tools.jvm.mvn;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.jcabi.xml.XPathContext;
import lombok.SneakyThrows;
import org.xembly.Directive;
import org.xembly.Directives;

import javax.xml.xpath.*;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class XemblerNs implements Iterable<Directive>  {


    public static final Pattern BRAKES = Pattern.compile("\"(.*?)\"");
    public static final String XPATH = "XPATH";

    /**
     * Ctor
     * @param dirs directives
     */
    public XemblerNs(Iterable<Directive> dirs, XPathQuery query) {
        this(dirs, new XPathContext().add(Pom.NS, Pom.NS_URL), query);
    }

    /**
     * Ctor.
     * @param dirs directives
     * @param context ctx
     */
    public XemblerNs(Iterable<Directive> dirs, XPathContext context, XPathQuery query) {
        this.context = context;
        this.dirs = verbsTransformed(dirs, query);
    }

    /**
     * Dirs
     */
    private final Iterable<Directive> dirs;

    /**
     * XPath
     */
    private final XPathContext context;


    @Override
    public Iterator<Directive> iterator() {
        return dirs.iterator();
    }

    @SneakyThrows
    private Directives verbsTransformed(Iterable<Directive> dirs, XPathQuery query) {
        initXPathFactory();

        List<String> verbs = Lists.newArrayList();
        dirs.iterator().forEachRemaining(expr -> {
            verbs.add(expr.toString());
        });

        for (int i = 0; i < verbs.size(); i++) {
            final String verb = verbs.get(i);
            if (verb.contains(XPATH)) {
                final Matcher matcher = BRAKES.matcher(verb);
                if (matcher.find()) {
                    final String xpath = matcher.group(1);
                    verbs.set(i, String.format(XPATH + " \"%s\"", query.apply(xpath)));
                }
            }
        }
        return new Directives(String.join(";", verbs) + ";");
    }


    public static class XPathFactoryDelegate extends XPathFactory {

        /**
         * Original factory
         */
        private final XPathFactory factory;

        /**
         * Context.
         */
        private final XPathContext context;

        /**
         * Ctor.
         * @param factory factory
         * @param context context
         */
        public XPathFactoryDelegate(XPathFactory factory, XPathContext context) {
            super();
            this.factory = factory;
            this.context = context;
        }

        @Override
        public boolean isObjectModelSupported(String objectModel) {
            return factory.isObjectModelSupported(objectModel);
        }

        @Override
        public void setFeature(String name, boolean value) throws XPathFactoryConfigurationException {
            factory.setFeature(name, value);
        }

        @Override
        public boolean getFeature(String name) throws XPathFactoryConfigurationException {
            return factory.getFeature(name);
        }

        @Override
        public void setXPathVariableResolver(XPathVariableResolver resolver) {
            factory.setXPathVariableResolver(resolver);
        }

        @Override
        public void setXPathFunctionResolver(XPathFunctionResolver resolver) {
            factory.setXPathFunctionResolver(resolver);
        }

        @Override
        public XPath newXPath() {
            final XPath xPath = factory.newXPath();
            xPath.setNamespaceContext(context);
            return xPath;
        }
    }




    private void initXPathFactory() {
        final XPathFactory that = XFACTORY.get();
        if (!(that instanceof XPathFactoryDelegate)) {
            final XPathFactoryDelegate delegate = new XPathFactoryDelegate(
                    that,
                    this.context
            );
            XFACTORY.set(delegate);
        }
    }

    private final static ThreadLocal<XPathFactory> XFACTORY;

    static {
        ThreadLocal<XPathFactory> tl = null;
        try {
            Class<?> clz = Class.forName("org.xembly.XpathDirective");
            @SuppressWarnings("JavaReflectionMemberAccess")
            final Field factory = clz.getDeclaredField("FACTORY");
            factory.setAccessible(true);
            @SuppressWarnings("unchecked")
            final ThreadLocal<XPathFactory> tmp = (ThreadLocal<XPathFactory>) factory.get(clz);
            tl = tmp;
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        XFACTORY = tl;
    }
}

