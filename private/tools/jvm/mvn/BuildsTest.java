package tools.jvm.mvn;

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

public class BuildsTest {


    @Test
    public void gen() {
        final Builds.PreOrderGraph graph = new Builds.PreOrderGraph(
                Lists.newArrayList(
                        new Builds.DefPom("/x", null),
                        new Builds.DefPom("/x/y", "/x"),
                        new Builds.DefPom("/x/z", "/x"),
                        new Builds.DefPom("/y", null),
                        new Builds.DefPom("/x/z/a1", "/x/z")
                )
        );

        String s = "\t| Builds.BuildNode(self=Builds.DefPom(file=/x, parentFile=null))\n" +
                "\t\t+-  Builds.BuildNode(self=Builds.DefPom(file=/x/y, parentFile=/x))\n" +
                "\t\t+-  Builds.BuildNode(self=Builds.DefPom(file=/x/z, parentFile=/x))\n" +
                "\t\t\t+-  Builds.BuildNode(self=Builds.DefPom(file=/x/z/a1, parentFile=/x/z))\n" +
                "\t| Builds.BuildNode(self=Builds.DefPom(file=/y, parentFile=null))\n";
        Assert.assertEquals(s, graph.toString());
    }
}
