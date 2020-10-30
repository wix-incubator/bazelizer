package tools.jvm.mvn;

import com.jcabi.xml.XPathContext;
import org.cactoos.collection.Joined;
import org.xembly.Directive;

import javax.xml.xpath.*;
import java.lang.reflect.Field;
import java.util.Iterator;

public final class DirectivesNs implements Iterable<Directive>  {

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

    /**
     * Ctor
     * @param dirs directives
     */
    @SafeVarargs
    public DirectivesNs(Iterable<Directive>...dirs) {
        this(new Joined<>(dirs), new XPathContext());
    }

    /**
     * Ctor.
     * @param dirs directives
     * @param context ctx
     */
    public DirectivesNs(Iterable<Directive> dirs, XPathContext context) {
        this.dirs = dirs;
        this.context = context;
    }

    /**
     * Dirs
     */
    private final Iterable<Directive> dirs;

    /**
     * XPath
     */
    private final XPathContext context;


    /**
     * Register new namespace
     * @param prefix prefix
     * @param uri uri
     * @return Namespaces dirs
     */
    public DirectivesNs registerNs(final String prefix, final Object uri) {
        return new DirectivesNs(
                this.dirs,
                this.context.add(prefix, uri)
        );
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


    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<Directive> iterator() {
        initXPathFactory();
        return dirs.iterator();
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
}

