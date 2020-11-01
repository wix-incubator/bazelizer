package tools.jvm.mvn;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import lombok.*;
import lombok.experimental.Accessors;
import org.cactoos.scalar.UncheckedScalar;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Accessors(fluent = true)
@Getter
@Builder(toBuilder = true)
@ToString
public final class Project {



    interface ProjectView {
        Iterable<Dep> deps();
        String groupId();
        String artifactId();
        String parent();

    }

    // TODO get rid of
    private String artifactId;
    private String groupId;

    /**
     * Dependencies.
     */
    @Builder.Default
    private Iterable<Dep> deps = ImmutableList.of();

    /**
     * Working directory.
     */
    private Path workDir;

    /**
     * Maven home directory.
     */
    @Builder.Default
    private Path m2Home = getTmpDirectory();

    /**
     * Output files for the project.
     */
    @Builder.Default
    private List<OutputFile> outputs = Lists.newArrayList();


    /**
     * Arguments for the maven.
     */
    @Builder.Default
    private Args args = new Args();

    /**
     * Pom template source.
     */
    private ByteSource pomTemplate;

    /**
     * Generated pom file.
     */
    private Path pom;

    /**
     * Parent pom file.
     * Not templating supported so far/
     */
    private Path parentPom;


    @Getter(AccessLevel.PRIVATE)
    private Path repositoryCached;

    public Path repository() {
        if (repositoryCached == null)
            repositoryCached = this.m2Home().resolve("repository");
        return repositoryCached;
    }


    public ProjectView toView() {
        return new ProjectView() {
            @Override
            public Iterable<Dep> deps() {
                return ImmutableList.copyOf(deps);
            }

            @Override
            public String groupId() {
                return groupId;
            }

            @Override
            public String artifactId() {
                return artifactId;
            }

            @Override
            public String parent() {
                if (parentPom != null) {
                    final Path pom = pom();
                    final Path relativize = pom.relativize(parentPom);
                    return relativize.toString();
                }
                return null;

            }
        };
    }


    public Pom.Props getPomProps() {
        return new UncheckedScalar<>(
                () -> new Pom.FromInput(
                        this.pom()
                ).props()
        ).value();
    }

    public Path getArtifactFolder() {
        final Pom.Props bean = getPomProps();
        return new Dep.Simple(null,
                bean.getGroupId(),
                bean.getArtifactId(),
                bean.getVersion()
        ).artifactFolder(repository());
    }


    public Path pom() {
        if (pom == null) {
            pom = syntheticPomFile(workDir);
        }
        return pom;
    }

    @SneakyThrows
    private static Path getTmpDirectory() {
        final String dirName = SysProps.labelHex().map(n ->
                "__" + Long.toHexString(System.currentTimeMillis()).toUpperCase()
                        + "__M2_HOME@" + n + "@").orElse("M2_HOME@");
        return Files.createTempDirectory(dirName);
    }

    public static Path syntheticPomFile(Path workDir) {
        for (int i = 0; i < 1000; i++) {
            final Path pom = workDir.resolve(
                    Texts.randomFileName("pom-synthetic") + "-" + i + ".xml"
            );
            if (Files.notExists(pom)) {
                return pom;
            }
        }
        throw new IllegalStateException();
    }

    public static String name() {
        return SysProps.labelHex().orElse("");
    }
}
