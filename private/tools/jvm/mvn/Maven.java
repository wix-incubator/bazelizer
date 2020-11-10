package tools.jvm.mvn;


import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
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

        private final String mvnDir;

        public BazelInvoker(String mavenBinary) {
            this.mvnDir = mavenBinary;
        }

        public BazelInvoker() {
            this(SysProps.getSysPropMavenTool());
        }

        @SneakyThrows
        @Override
        public void run(Project build) {
            final File mavenHome = new File(mvnDir);

            DefaultInvoker invoker = new DefaultInvoker();
            invoker.setMavenHome(mavenHome);
            invoker.setWorkingDirectory(build.workDir().toFile());

            log.debug("maven home exists: {} - {}", mavenHome.exists(),mavenHome);
            log.info("execute: {}", build.args());

            final InvocationRequest request = build.args().toInvocationRequest();
            final Path pomFilePath = build.pom();
            request.setPomFile(pomFilePath.toFile());
            request.setJavaHome(new File(System.getProperty("java.home")));
            request.setBatchMode(true);
            request.setShowVersion(true);

            setLogLevel(request);
            final String id = SLF4JConfigurer.shortMDC(pomFilePath);
            final InvocationResult result = SLF4JConfigurer.withMDC(id, () -> invoker.execute(request));
            if (result.getExitCode() != 0) {
                log.error("Build failed");
                log.error("================ Project ===============");
                log.error("{}", build.debug());
                log.error("repository state:");
                Files.walk(build.repository(), 2).limit(45).forEach(f -> {
                    log.error("-> {}", f);
                });
                log.error("========================================");
                throw new ToolMavenInvocationException(result);
            }
        }

        private static void setLogLevel(InvocationRequest request) {
            switch (SLF4JConfigurer.getLogLevel()) {
                case OFF:
                    Properties properties = request.getProperties();
                    if (properties == null) {
                        properties = new Properties();
                    }
                    properties.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN");
                    request.setProperties(properties);
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
