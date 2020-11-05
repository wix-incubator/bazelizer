package tools.jvm.mvn;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class XemblerAugmentTest {


    public XemblerAugmentTest(String xpath, String resXpath) {
        this.xpath = xpath;
        this.resXpath = resXpath;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> primeNumbers() {
        return Arrays.asList(new Object[][] {
                { "/project/comment()", "/xe:project/comment()" },
                { "/comment()", "/comment()" },
                { "/*/:ns:", "/*/:ns:" },
                { "/project/group/text()", "/xe:project/xe:group/text()" },
                { "/project/group[v=\"1\"]/text()", "/xe:project/xe:group[v=\"1\"]/text()" },
        });
    }

    private final String xpath;
    private final String resXpath;

    @Test
    public void testXPath() {
        Assert.assertEquals(resXpath, new XemblerAugment.XPathQueryPref("xe").apply(xpath));
    }
}
