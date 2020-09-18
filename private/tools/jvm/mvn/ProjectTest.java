package tools.jvm.mvn;

import org.junit.Assert;
import org.junit.Test;


public class ProjectTest {
    @Test
    public void test()  {
        final String key = "--artifact_id";

        Project p = new Project() {
            @Override
            public String artifactId() {
                return this.lazy().groupId() + key;
            }
        };


        final Project lazy = p.lazy();
        Assert.assertEquals(lazy.groupId()+key, p.artifactId());
    }
}
