package tools.jvm.mvn;


import com.google.devtools.build.runfiles.Runfiles;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

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
            String mavenBinRunfileDir = runfiles.rlocation(ws);

            DefaultInvoker invoker = new DefaultInvoker();
            invoker.setMavenHome(new File(mavenBinRunfileDir));
            invoker.setWorkingDirectory(build.workDir().toFile());

            log.info("execute: {}", build.args());

            final InvocationRequest request = build.args().toInvocationRequest();
            request.setPomFile(build.pom().toFile());
            request.setJavaHome(new File(System.getProperty("java.home")));
            request.setLocalRepositoryDirectory(build.repository().toFile());
            request.setBatchMode(true);

            if (build.args().offline()) {
                request.setOffline(true);
            }

            setLogLevel(request);
            invoker.execute(request);
        }

        private static void setLogLevel(InvocationRequest request) {
            switch (SLF4JConfigurer.getLogLevel()) {
                case OFF:
                    Properties properties = request.getProperties();
                    if (properties == null) {
                        properties = new Properties();
                        request.setProperties(properties);
                    }
                    final Properties props = new Properties();
                    props.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN");
                    request.setProperties(props);
                    break;
                case INFO:
                case DEBUG: break;
                case TRACE:
                    request.setDebug(true);
                    break;
            }
        }
    }
}
