package tools.jvm.v2.mvn;

import org.junit.Test;

import java.nio.file.Paths;

public class PomFilesTest {

    @Test
    public void build() {
        final PomFiles pomFiles = new PomFiles();
        pomFiles.addFile(Paths.get("/Users/bohdans/Projects/bazelizer/tests/e2e/mvn-build-lib/pom.xml"));
    }
}
