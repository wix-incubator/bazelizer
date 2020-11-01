package tools.jvm.mvn;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.cactoos.Text;
import org.cactoos.io.InputOf;
import org.cactoos.io.InputStreamOf;
import org.cactoos.text.TextOf;
import org.w3c.dom.Node;
import org.xembly.Directives;
import org.xembly.Xembler;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

public interface Template<K> {


    /**
     * Evaluate template.
     *
     * @return char source
     */
    Text eval(Text tpl, K been);

    /**
     * Template envelope via mustache.
     */
    class Mustache<K> implements Template<K> {
        private static final MustacheFactory MF = new DefaultMustacheFactory();


        @Override
        @SneakyThrows
        public Text eval(Text input, K bean) {
            try (Reader tpl = new InputStreamReader(new InputStreamOf(input))) {
                final StringWriter str = new StringWriter();
                try (Writer dest = new PrintWriter(str)) {
                    com.github.mustachejava.Mustache mustache = MF.compile(tpl, "template.mustache");
                    mustache.execute(dest, bean);
                }

                return new TextOf(new InputOf(str.toString()));
            }
        }
    }

}
