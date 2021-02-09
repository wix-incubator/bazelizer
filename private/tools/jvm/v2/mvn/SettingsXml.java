package tools.jvm.v2.mvn;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cactoos.Input;
import org.cactoos.io.InputOf;
import org.cactoos.io.ReaderOf;
import org.cactoos.io.ResourceOf;

import java.io.*;
import java.nio.file.Path;
import java.util.Collections;

public interface SettingsXml {

    Input render(Path repository) throws IOException;


    @Slf4j
    @AllArgsConstructor
    class Default implements SettingsXml {
        private static final MustacheFactory MF = new DefaultMustacheFactory();
        private final Iterable<Profile> profiles;

        public Default() {
            this(Collections.emptyList());
        }

        @Getter
        public static class Profile {
            private String id;
            private String url;
        }

        @Override
        public Input render(Path repository) throws IOException {
            try (Reader tpl = new ReaderOf(new ResourceOf("settings.xml.mustache"))) {
                final StringWriter str = new StringWriter();
                try (Writer dest = new PrintWriter(str)) {
                    com.github.mustachejava.Mustache mustache = MF.compile(tpl, "template.mustache");
                    final ImmutableMap<String, Object> props = ImmutableMap.of(
                            "localRepository", repository.toString(),
                            "profiles", profiles
                    );
                    mustache.execute(dest, props);
                }
                log.info("settings.xml:\n{}", str);
                return new InputOf(str.toString());
            }
        }
    }


    @AllArgsConstructor
    @Slf4j
    class Json implements SettingsXml {
        private final SettingsXml aDefault;
        public Json(Path p) {
            this.aDefault = new Default(
                    new Manifest(p).items(Default.Profile.class)
            );
        }

        @Override
        public Input render(Path repository) throws IOException {
            return aDefault.render(repository);
        }
    }

}
