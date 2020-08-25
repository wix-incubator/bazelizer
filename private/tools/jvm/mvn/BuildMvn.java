package tools.jvm.mvn;

import com.google.common.io.ByteSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.cli.MavenCli;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

@Slf4j
public class BuildMvn  {

    public void run(Project project) {
        Std out = new Std();
        final MavenCli cli = new MavenCli();
        final Args args = project.args();
        final String basedir = project.workDir().toAbsolutePath().toString();
        System.setProperty("maven.multiModuleProjectDirectory", basedir);
        log.info("\"mvn\" command: {}", args);
        int status = cli.doMain(args.toArray(), basedir, out.stdout, out.stderr);
        if (status != 0) {
            out.log();
            throw new BuildException("\"mvn\" -- non zero exit code " + status);
        }
    }


    private static class Std {
        private static final Logger mavenLOG = LoggerFactory.getLogger("org.apache.maven");
        private final ByteArrayOutputStream bosStdout = new ByteArrayOutputStream();
        private final ByteArrayOutputStream bosStderr = new ByteArrayOutputStream();

        final PrintStream stdout;
        final PrintStream stderr;

        public Std() {
            if (mavenLOG.isInfoEnabled()) {
                stdout = System.out;
                stderr = System.err;
            } else {
                stdout = new PrintStream(bosStdout);
                stderr = new PrintStream(bosStderr);
            }
        }

        @lombok.SneakyThrows
        void log() {
            if (stdout != System.out) {
                ByteSource.wrap(bosStdout.toByteArray()).copyTo(System.out);
                ByteSource.wrap(bosStderr.toByteArray()).copyTo(System.err);
                if (bosStdout.size() > 0) {
                    System.out.println();
                    System.out.flush();
                }
                if (bosStderr.size() > 0) {
                    System.err.println();
                    System.err.flush();
                }
            }
        }
    }
}
