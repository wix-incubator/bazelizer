package tools.jvm.mvn;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.jcabi.xml.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.cactoos.Text;
import org.cactoos.io.InputOf;
import org.cactoos.text.TextOf;
import org.w3c.dom.Node;
import org.xembly.Xembler;

import java.io.*;
import java.nio.charset.StandardCharsets;

public interface Template {

    /**
     * Evaluate template.
     * @return char source
     */
    Text eval();


    @Data
    static class PomPropsBean {
        final String groupId;
        final String artifactId;
        final String version;
    }

    /**
     * Template envelope via mustache.
     */
    @AllArgsConstructor
    class Mustache implements Template {

        private final ByteSource source;

        private final Object bean;

        @Override
        @SneakyThrows
        public Text eval() {
            final CharSource tplSource = source.asCharSource(StandardCharsets.UTF_8);
            MustacheFactory mf = new DefaultMustacheFactory();
            try (Reader tpl = tplSource.openStream()) {
                final StringWriter str = new StringWriter();
                try (Writer dest = new PrintWriter(str)) {
                    com.github.mustachejava.Mustache mustache = mf.compile(tpl, "template.mustache");
                    mustache.execute(dest, bean);
                }

                return new TextOf(new InputOf(str.toString()));
            }
        }
    }

    @AllArgsConstructor
    class Xe implements Template {

        private final Pom source;

        private final XePom dir;

        @SneakyThrows
        @Override
        public Text eval() {
            final Node newNode = new Xembler(dir.value()).applyQuietly(source.xml().node());
            return new TextOf(
                    new XMLDocument(newNode).toString()
            );
        }
    }


}
