package tools.jvm.mvn;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

public class BuildsTest {


    @Test
    public void gen() {
        final Builds.PreOrderGraph graph = new Builds.PreOrderGraph(
                Lists.newArrayList(
                        new Builds.PomDefinition("/x", null),
                        new Builds.PomDefinition("/x/y", "/x"),
                        new Builds.PomDefinition("/x/z", "/x"),
                        new Builds.PomDefinition("/y", null),
                        new Builds.PomDefinition("/x/z/a1", "/x/z")
                )
        );

        String s = "\t| BuildNode{file=/x, parent=null}\n" +
                "\t\t+- BuildNode{file=/x/y, parent=/x}\n" +
                "\t\t+- BuildNode{file=/x/z, parent=/x}\n" +
                "\t\t\t+- BuildNode{file=/x/z/a1, parent=/x/z}\n" +
                "\t| BuildNode{file=/y, parent=null}\n";
        Assert.assertEquals(s, graph.toString());
    }
}
