package tools.jvm.mvn;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import lombok.Data;
import lombok.experimental.Accessors;
import org.cactoos.Input;
import org.cactoos.Output;
import org.cactoos.io.InputOf;
import org.cactoos.io.InputStreamOf;
import org.xembly.Directives;
import org.xembly.Xembler;

import java.io.IOException;
import java.nio.file.Path;

public class RunManifest {

    /**
     * Builder.
     */
    @Data
    @Accessors(fluent = true)
    static class Builder {
        private String settingsXmlTemplate;

        public RunManifest build() {
            return new RunManifest(
                    new XMLDocument(
                            new Xembler(new Directives()
                                    .add("global")
                                    .add("settings_tpl").set(Xembler.escape(settingsXmlTemplate))
                            ).domQuietly()
                    )
            );
        }
    }

    /**
     * Ctor.
     * @param file run manifest as a fs xml doc
     * @throws IOException
     */
    public RunManifest(Path file) throws IOException {
        this(new XMLDocument(new InputStreamOf(file)));
    }

    /**
     * Ctor.
     * @param in run manifest as input
     * @throws IOException if any
     */
    public RunManifest(Input in) throws IOException {
        this(new XMLDocument(new InputStreamOf(in)));
    }

    /**
     * Ctor.
     * @param xml source of run manifest
     */
    public RunManifest(XML xml) {
        this.xml = xml;
    }

    private final XML xml;


    public String getSettingsXml() {
        return xml.xpath("//global/settings_tpl/text()").get(0);
    }

    public XML asXML() {
        return this.xml;
    }

    public String asString() {
        return new Pom.PrettyPrintXml(this.xml).asString();
    }

    public Input asBinary() {
        return new InputOf(asString());
    }
}
