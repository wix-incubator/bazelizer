package tools.jvm.mvn;

import org.junit.Assert;
import org.junit.Test;

public class ArgsTest {

    @Test
    public void createFromCmd() {
        final Args args = new Args().parseCommandLine("-P vespa_6,bazel --goals=compile");
        final String s = args.toString();
        Assert.assertEquals("Args{goals=[compile], profiles=[vespa_6, bazel], ctx={}}", s);
    }
}
