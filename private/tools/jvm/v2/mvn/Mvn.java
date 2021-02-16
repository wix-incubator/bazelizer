package tools.jvm.v2.mvn;

import com.google.common.hash.Hashing;
import com.google.devtools.build.runfiles.Runfiles;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationResult;
import org.cactoos.Input;
import org.cactoos.Scalar;
import org.cactoos.io.BytesOf;
import org.cactoos.io.InputOf;
import org.cactoos.scalar.UncheckedScalar;
import org.cactoos.text.TextOf;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("UnstableApiUsage")
@Slf4j
public class Mvn {
    private static final String PREF = "tools.jvm.mvn.";
    private static final String BZL_NAME_SYS_PROP = PREF + "BazelLabelName";
    private static final String BZL_MVN_TOOL_SYS_PROP = PREF + "MavenBin";

    public static final UncheckedScalar<String> LABEL;
    public static final UncheckedScalar<File> MAVEN_TOOL;

    private static Scalar<Runfiles> _sRunfiles;


    public static IOFileFilter REPOSITORY_FILES_FILTER = FileFilterUtils.and(
            FileFilterUtils.fileFileFilter(),
            // SEE: https://stackoverflow.com/questions/16866978/maven-cant-find-my-local-artifacts
            //
            //So with Maven 3.0.x, when an artifact is downloaded from a repository,
            // maven leaves a _maven.repositories file to record where the file was resolved from.
            //
            //Namely: when offline, maven 3.0.x thinks there are no repositories, so will always
            // find a mismatch against the _maven.repositories file
            FileFilterUtils.notFileFilter(
                    FileFilterUtils.prefixFileFilter("_remote.repositories")
            )
    );

    static {
        _sRunfiles = memoize(Mvn::runfiles);

        LABEL = new UncheckedScalar<>(
                memoize(() -> Optional.ofNullable(System.getProperty(BZL_NAME_SYS_PROP))
                        .map(name -> Hashing.murmur3_32().hashString(name, StandardCharsets.UTF_8).toString().toUpperCase())
                        .orElseThrow(() -> new IllegalStateException("no sys prop: " + BZL_NAME_SYS_PROP)))
        );

        MAVEN_TOOL = new UncheckedScalar<>(
                memoize(() -> Optional.ofNullable(System.getProperty(BZL_MVN_TOOL_SYS_PROP))
                        .map(runfilesPath -> new File(new UncheckedScalar<>(_sRunfiles).value().rlocation(runfilesPath)))
                        .orElseThrow(() -> new IllegalStateException("no sys prop " + BZL_MVN_TOOL_SYS_PROP)))
        );
    }

    /**
     * Bazel runfiles.
     */
    @SneakyThrows
    private static Runfiles runfiles()  {
        return Runfiles.create();
    }

    /**
     * Error.
     */
    public static class MvnException extends RuntimeException {
        public MvnException(String message) {
            super(message);
        }
    }

    /**
     * Ctor.
     * @param xml settings xml
     */
    public Mvn(SettingsXml xml) {
        this(null, xml);
    }

    /**
     * Ctor.
     * @param repositoryTar repository snapshot
     */
    public Mvn(Input repositoryTar, SettingsXml settingsXml) {
        this.m2 = memoize(() -> {
            Path m2HomeDir = Files.createTempDirectory("M2_HOME@_" + LABEL.value() + "_@");
            Path repository = m2HomeDir.resolve("repository").toAbsolutePath();
            Files.createDirectories(repository);
            Path settingsXmlFile = m2HomeDir.resolve("settings.xml").toAbsolutePath();
            final Input settingsXmlContent = settingsXml.render(repository);
            log.info(" [settings.xml]  {}", settingsXmlFile);
            log.info(" [settings.xml] \n{}", new TextOf(settingsXmlContent).asString());

            Files.write(settingsXmlFile, new BytesOf(settingsXmlContent).asBytes());
            if (repositoryTar != null)
                TarUtils.untar(repositoryTar, repository);
            return m2HomeDir;
        });
    }

    /**
     * State.
     */
    private final Scalar<Path> m2;


    public void execOffline(File pomFile, List<String> cmd, List<String> profiles) {
        execute(pomFile, cmd, profiles, true);
    }

    public void exec(File pomFile, List<String> cmd, List<String> profiles) {
        execute(pomFile, cmd, profiles, false);
    }

    @SneakyThrows
    private void execute(File pomFile, List<String> cmd, List<String> profiles, boolean offline) {
        DefaultInvoker invoker = new DefaultInvoker();
        invoker.setMavenHome(Mvn.MAVEN_TOOL.value());
        invoker.setWorkingDirectory(new File(pomFile.getParent()));

        final DefaultInvocationRequest request = this.newRequest();
        request.setPomFile(pomFile);
        request.setGoals(cmd);
        request.setProfiles(profiles);
        request.setOffline(offline);
        Properties properties = request.getProperties();
        if (properties == null) {
            properties = new Properties();
        }
        properties.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN");
        request.setProperties(properties);

        log.info("running {}", cmd);
        log.info("");
        final InvocationResult execute = invoker.execute(request);
        if (execute.getExitCode() != 0) {
            throw new MvnException("exit code " + execute.getExitCode() + ";");
        }
    }

    /**
     * Settings xml location.
     */
    public Path settingsXml() {
        return m2HomeDir().resolve("settings.xml").toAbsolutePath();
    }

    /**
     * Repository location.
     */
    public Path repository() {
        return m2HomeDir().resolve("repository").toAbsolutePath();
    }


    /**
     * M2 HOME.
     */
    @SneakyThrows
    private Path m2HomeDir() {
        return m2.value();
    }

    /**
     * Install archive content into repository.
     * @param tar tar archive
     */
    @SuppressWarnings("unused")
    private void installRepo(Path tar) {
        TarUtils.untar(new InputOf(tar), repository());
    }

    /**
     * Install deps
     * @param deps deps
     */
    public void installDeps(Iterable<Dep> deps) {
        for (Dep dep : deps) {
            Path artifactFolder = repository().resolve(mvnLayout(dep));
            //noinspection ResultOfMethodCallIgnored
            artifactFolder.toFile().mkdirs();
            addPom(artifactFolder, dep);
            copyJar(artifactFolder, dep);
        }
    }

    public static Path mvnLayout(Dep dep) {
        String[] gidParts = dep.getGroupId().split("\\.");
        Path thisGroupIdRepo = Paths.get("");
        for (String gidPart : gidParts) {
            thisGroupIdRepo = thisGroupIdRepo.resolve(gidPart);
        }
        return thisGroupIdRepo.resolve(dep.getArtifactId()).resolve(dep.getVersion());
    }


    @SneakyThrows
    private void addPom(Path artifactFolder, Dep dep) {
        String fileName = dep.getArtifactId() + "-" + dep.getVersion();
        Path pomFile = artifactFolder.resolve(fileName + ".pom");
        String pom = "<project>\n" +
                "<modelVersion>4.0.0</modelVersion>\n" +
                "<groupId>" + dep.getGroupId() + "</groupId>\n" +
                "<artifactId>" + dep.getArtifactId() + "</artifactId>\n" +
                "<version>" + dep.getVersion() + "</version>\n" +
                "<description>Generated by " + this.getClass() + " for " + dep + "</description>\n" +
                "</project>";
        Files.write(pomFile, pom.getBytes(StandardCharsets.UTF_8));
        log.debug("[deps] install {}\n{}", pomFile, pom);
    }

    @SneakyThrows
    private void copyJar(Path artifactFolder, Dep dep) {
        Path jarFile = artifactFolder.resolve(dep.getArtifactId() + "-" + dep.getVersion() + ".jar");
        Files.copy(dep.getSource(), jarFile, StandardCopyOption.REPLACE_EXISTING);
    }


    private DefaultInvocationRequest newRequest() {
        final DefaultInvocationRequest request = new DefaultInvocationRequest();
        request.setUserSettingsFile(settingsXml().toFile());
        request.setLocalRepositoryDirectory(repository().toFile());
        request.setJavaHome(new File(System.getProperty("java.home")));
        request.setBatchMode(true);
        request.setShowVersion(true);
        return request;
    }


    public static <T> Scalar<T> memoize(Scalar<T> delegate) {
        AtomicReference<T> value = new AtomicReference<>();
        return () -> {
            T val = value.get();
            if (val == null) {
                synchronized(value) {
                    val = value.get();
                    if (val == null) {
                        val = Objects.requireNonNull(delegate.value());
                        value.set(val);
                    }
                }
            }
            return val;
        };
    }
}
