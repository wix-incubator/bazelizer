package tools.jvm.mvn;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;

import java.io.*;
import java.nio.charset.StandardCharsets;

public interface TemplateEnvelop {

    CharSource eval();


    /**
     * Mustache template with data resolved from pom.xml
     */
    class PomBased extends Mustache implements TemplateEnvelop {
        @Data
        static class Props {
            final String groupId;
            final String artifactId;
            final String version;
        }

        @SuppressWarnings("UnstableApiUsage")
        public PomBased(String source, Project project) {
            super(CharSource.wrap(source).asByteSource(StandardCharsets.UTF_8), getBean(project));
        }

        @SneakyThrows
        private static Props getBean(Project project) {
            final XML xml = new XMLDocument(project.pom().toFile());
            String gid =  xml.xpath("/project/groupId/text()").get(0);
            String aid =  xml.xpath("/project/artifactId/text()").get(0);
            String v =  xml.xpath("/project/version/text()").get(0);
            return new Props(gid, aid, v);
        }
    }

    /**
     * Template envelope via mustache.
     */
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
