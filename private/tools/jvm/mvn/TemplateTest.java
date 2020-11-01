package tools.jvm.mvn;

import com.google.common.collect.ImmutableMap;
import org.cactoos.text.TextOf;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class TemplateTest {

    @Test
    public void eval() throws IOException {
        final String x = new Template.Mustache<Object>().eval(
                new TextOf("Hello {{ x }}"),
                ImmutableMap.of("x", "142")
        ).asString();

        Assert.assertEquals("Hello 142", x);
    }

}
