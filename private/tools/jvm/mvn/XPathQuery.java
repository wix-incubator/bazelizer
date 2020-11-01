package tools.jvm.mvn;

import com.google.common.collect.Lists;
import com.jcabi.xml.XML;

import java.util.List;
import java.util.function.Function;

public class XPathQuery {

    private final Function<String,String> map;

    public XPathQuery(XML xml) {
        final List<String> namespaces = xml.xpath("/*/namespace::*[name()='']");
        Function<String,String> map = Function.identity();
        if (namespaces.contains(Pom.NS_URL)) {
            map = (query) -> {
                final String[] split = query.split("/");
                final List<String> tokens = Lists.newArrayList();
                for (String token : split) {
                    if (token.isEmpty() || token.contains("()")) {
                        tokens.add(token);
                    } else {
                        tokens.add(Pom.NS + ":" + token);
                    }
                }
                return String.join("/", tokens);
            };
        }
        this.map = map;
    }

    public String apply(String query) {
        return this.map.apply(query);
    }
}
