package tools.jvm.mvn;

import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.cactoos.Input;
import org.cactoos.Scalar;

import java.io.InputStream;
import java.util.List;

public interface Pom {

    @Data
    class Props {
        final String groupId;
        final String artifactId;
        final String version;
    }


    /**
     * Pom as XMP
     * @return xml
     * @throws Exception if any error
     */
    XML xml() throws Exception ;

    @AllArgsConstructor
    class XPath implements Pom {

        private final Input data;

        @Override
        public XML xml() throws Exception {
            try (InputStream src = data.stream()) {
                return new XMLDocument(src).registerNs("pom", "http://maven.apache.org/POM/4.0.0");
            }
        }

        public Props props() throws Exception {
            XML xml = xml();
            final List<String> namespaces = xml.xpath("/*/namespace::*[name()='']");
            if (!namespaces.isEmpty()) {
                String gid =  xml.xpath("/pom:project/pom:groupId/text()").get(0);
                String aid =  xml.xpath("/pom:project/pom:artifactId/text()").get(0);
                String v =  xml.xpath("/pom:project/pom:version/text()").get(0);
                return new Props(gid, aid, v);
            } else {
                String gid =  xml.xpath("/project/groupId/text()").get(0);
                String aid =  xml.xpath("/project/artifactId/text()").get(0);
                String v =  xml.xpath("/project/version/text()").get(0);
                return new Props(gid, aid, v);
            }
        }
    }

}
