package tools.jvm.v2.mvn;

import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

public class PomFilesTest {

    @Test
    public void build() {
        final PomFiles pomFiles = new PomFiles();
        pomFiles.registerFile(Paths.get("/Users/bohdans/Projects/bazelizer/tests/e2e/mvn-build-lib/pom.xml"));

        final PomFiles.BuildsOrder builds = pomFiles.builds();
        System.out.println(builds);

        builds.each(pf -> {
            final File location = pf.persisted(false);
            System.out.println(location);

            final Pom pom = pf.pom();
            System.out.println(pom.asString());
        });

    }
}
