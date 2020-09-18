package tools.jvm.mvn;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import java.io.*;
import java.nio.charset.StandardCharsets;

public interface TemplateEnvelop {

    CharSource eval();


    @AllArgsConstructor
    class Mustache implements TemplateEnvelop {
        private final ByteSource source;
        private final Object bean;

        @Override
        @SneakyThrows
        public CharSource eval() {
            final CharSource tplSource = source.asCharSource(StandardCharsets.UTF_8);
            MustacheFactory mf = new DefaultMustacheFactory();
            try (Reader tpl = tplSource.openStream()) {
                final StringWriter str = new StringWriter();
                try (Writer dest = new PrintWriter(str)) {
                    com.github.mustachejava.Mustache mustache = mf.compile(tpl, "template.mustache");
                    mustache.execute(dest, bean);
                }

                return new CharSource() {
                    @Override
                    public Reader openStream() throws IOException {
                        return new StringReader(str.toString());
                    }
                };
            }
        }
    }
}
