package tools.jvm.v2.mvn;

import com.google.common.hash.Hashing;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.cactoos.Input;
import org.cactoos.Scalar;
import org.cactoos.io.BytesOf;
import org.cactoos.io.DeadInput;
import org.cactoos.io.InputOf;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
@Slf4j
public class Maven {
    public static final String PREF = "tools.jvm.mvn.";
    private static final String BZL_NAME_SYS_PROP = PREF + "BazelLabelName";

    public static final String LABEL;
    public static final File MAVEN_TOOL;
    static {
        LABEL = Optional.ofNullable(System.getProperty(BZL_NAME_SYS_PROP))
                .map(name -> Hashing.murmur3_32().hashString(name, StandardCharsets.UTF_8).toString())
                .orElseThrow(() -> new IllegalStateException("no sys prp: " + BZL_NAME_SYS_PROP));
        MAVEN_TOOL = new File("");
    }

    interface InvocationRequests extends Supplier<InvocationRequest> {
    }


    /**
     * Ctor.
     * @throws IOException if any
     */
    public Maven() throws IOException {
        this(new DeadInput());
    }

    /**
     * Ctor.
     * @param repositoryTar repository snapshot
     * @throws IOException on any error
     */
    public Maven(Input repositoryTar) throws IOException {
        this.m2 = memoize(() -> {
            Path m2HomeDir = Files.createTempDirectory("M2_HOME@_" + LABEL + "_@");
            Path repository = repository();
            Files.createDirectories(repository);
            generateSettingsXml();
            TarUtils.untar(repositoryTar, repository);
            return m2HomeDir;
        });
    }

    /**
     * State.
     */
    private final Scalar<Path> m2;


    public Build buildOf(Path srcPomFile) {
        return new Build() {

            private Pom pom;

            @SneakyThrows
            @Override
            public void exec() {
                Path pomFile = srcPomFile.getParent().resolve(
                        "pom." + LABEL + ".xml"
                );
                if (Files.notExists(pomFile)) {
                    Files.write(pomFile, pom.toString().getBytes());
                }

                DefaultInvoker invoker = new DefaultInvoker();
                invoker.setMavenHome(MAVEN_TOOL);
                invoker.setWorkingDirectory(pomFile.getParent().toFile());

                final DefaultInvocationRequest request = newRequest();
                request.setPomFile(pomFile.toFile());
                invoker.execute(request);
            }

            @Override
            public void addDeps(Iterable<Dep> deps) {
                installDeps(deps);
                pom = pom.withDirectives(
                        new PomUpdate.PomStruc(),
                        new PomUpdate.PomDropDeps(),
                        new PomUpdate.AppendDeps(deps)
                );
            }
        };
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


    @SneakyThrows
    private void generateSettingsXml()  {
        Path settingsXml = settingsXml();
        final String xml = ""; // TODO
        Files.write(settingsXml, new BytesOf(xml).asBytes());
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
    private void installRepo(Path tar) {
        TarUtils.untar(new InputOf(tar), repository());
    }

    /**
     * Install deps
     * @param deps deps
     */
    private void installDeps(Iterable<Dep> deps) {
        Function<Dep, Path> mvnLayout = dep -> {
            String[] gidParts = dep.getGroupId().split("\\.");
            Path thisGroupIdRepo = Paths.get("");
            for (String gidPart : gidParts) {
                thisGroupIdRepo = thisGroupIdRepo.resolve(gidPart);
            }
            return thisGroupIdRepo.resolve(dep.getArtifactId()).resolve(dep.getVersion());
        };

        for (Dep dep : deps) {
            Path artifactFolder = repository().resolve(mvnLayout.apply(dep));
            //noinspection ResultOfMethodCallIgnored
            artifactFolder.toFile().mkdirs();
            addPom(artifactFolder, dep);
            copyJar(artifactFolder, dep);
        }
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
        log.debug("write pom {}\n{}", pomFile, pom);
    }

    @SneakyThrows
    private void copyJar(Path artifactFolder, Dep dep) {
        Path jarFile = artifactFolder.resolve(dep.getArtifactId() + "-" + dep.getVersion() + ".jar");
        Files.copy(dep.getSource(), jarFile, StandardCopyOption.REPLACE_EXISTING);
    }


    public DefaultInvocationRequest newRequest() {
        final DefaultInvocationRequest request = new DefaultInvocationRequest();
        // request.setGoals(Lists.newArrayList(Sets.newLinkedHashSet(goals)));
        // request.setProfiles(Lists.newArrayList(Sets.newLinkedHashSet(profiles)));
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
