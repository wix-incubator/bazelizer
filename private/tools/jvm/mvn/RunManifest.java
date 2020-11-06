package tools.jvm.mvn;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import lombok.Data;
import lombok.experimental.Accessors;
import org.cactoos.io.InputStreamOf;
import org.xembly.Directives;
import org.xembly.Xembler;

import java.io.IOException;
import java.nio.file.Path;

public class RunManifest {

    @Data
    @Accessors(fluent = true)
    static class Builder {
        private String settingsXmlTemplate;

        public RunManifest build() {
            return new RunManifest(
                    new XMLDocument(
                            new Xembler(new Directives()
                                    .add("global")
                                    .add("settings_tpl").cdata(settingsXmlTemplate)
                            ).domQuietly()
                    )
            );
        }
    }

    public RunManifest(Path file) throws IOException {
        this(new XMLDocument(new InputStreamOf(file)));
    }

    public RunManifest(XML xml) {
        this.xml = xml;
    }

    private final XML xml;


    public String getSettingsXml() {
        return xml.xpath("/global/settings_mustache/text()").get(0);
    }

    public XML asXML() {
        return this.xml;
    }

    public String asString() {
        return asXML().toString();
    }
}
