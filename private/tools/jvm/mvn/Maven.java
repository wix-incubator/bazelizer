package tools.jvm.mvn;


import com.google.devtools.build.runfiles.Runfiles;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.Invoker;

import java.io.File;
import java.util.Arrays;

public interface Maven {

    /**
     * Run specified project by maven.
     * @param build project
     */
    void run(Project build);


    /**
     *
     * Execute maven invoker from Bazel environment.
     */
    @Slf4j
    class BazelInvoker implements Maven {

        @SneakyThrows
        @Override
        public void run(Project build) {
            final String ws = System.getProperty("maven.bin.workspace");
            Runfiles runfiles = Runfiles.create();
            String path = runfiles.rlocation(ws + "/bin/mvn");
            log.info("Invoke exec " + path);

            DefaultInvoker invoker = new DefaultInvoker();
            invoker.setMavenExecutable(new File(path));
            invoker.setMavenHome(build.m2Home().toFile());
            invoker.setWorkingDirectory(build.workDir().toFile());

            final DefaultInvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(build.pom().toFile());
            request.setJavaHome(new File(System.getProperty("java.home")));
            request.setGoals(Arrays.asList(build.args().toArray()));
            request.setLocalRepositoryDirectory(build.m2Home().resolve("repository").toFile());
            request.setBatchMode(true);

            if (build.args().offline()) {
                request.setOffline(true);
            }

            invoker.execute(request);
        }
    }
}
