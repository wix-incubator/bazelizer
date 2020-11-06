package tools.jvm.mvn;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.jcabi.xml.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.cactoos.Input;
import org.cactoos.Scalar;
import org.cactoos.Text;
import org.cactoos.io.InputOf;
import org.cactoos.io.ReaderOf;
import org.cactoos.text.TextOf;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
        private static final MustacheFactory mf = new DefaultMustacheFactory();

        private final Input source;
        private final Object bean;

        @Override
        @SneakyThrows
        public Text eval() {
            try (Reader tpl = new ReaderOf(source)) {
                final StringWriter str = new StringWriter();
                try (Writer dest = new PrintWriter(str)) {
                    com.github.mustachejava.Mustache mustache = mf.compile(tpl, "template.mustache");
                    mustache.execute(dest, bean);
                }

                return new TextOf(new InputOf(str.toString()));
            }
        }
    }
}
