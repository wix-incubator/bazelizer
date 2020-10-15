package tools.jvm.mvn;


import com.google.common.collect.Lists;
import com.google.devtools.build.runfiles.Runfiles;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.SystemOutHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
     * Guard.
     */
    @AllArgsConstructor
    class InstallGuard implements Maven {

        private final Maven maven;

        @SneakyThrows
        @Override
        public void run(Project build) {
            IOFileFilter filter = FileFilterUtils.and(
                    FileFilterUtils.fileFileFilter(),
                    FileFilterUtils.suffixFileFilter(".jar"));
            FileAlterationObserver observer = new FileAlterationObserver(build.repository().toFile(), filter);
            observer.addListener(new FileAlterationListenerAdaptor() {
                @Override
                public void onFileCreate(File file) {
                    System.out.println("File installed: " + file);
                }

                @Override
                public void onFileChange(File file) {
                    System.out.println("File change: " + file);
                }
            });
            observer.initialize();
            maven.run(build);
            Thread.sleep(100);
            observer.checkAndNotify();
            observer.destroy();
        }
    }

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
            String mavenBinRunfileDir = runfiles.rlocation(ws );

            DefaultInvoker invoker = new DefaultInvoker();
            invoker.setMavenHome(new File(mavenBinRunfileDir));
            invoker.setWorkingDirectory(build.workDir().toFile());
            final DefaultInvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(build.pom().toFile());
            request.setJavaHome(new File(System.getProperty("java.home")));
            final List<String> args = Arrays.asList(build.args().toArray());
            log.info("execute: {}", args);
            request.setGoals(args);
            request.setLocalRepositoryDirectory(build.repository().toFile());
            request.setBatchMode(true);

            if (build.args().offline()) {
                request.setOffline(true);
            }
            setLogLevel(request);
            invoker.execute(request);
        }

        private static void setLogLevel(DefaultInvocationRequest request) {
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
