package tools.jvm.mvn;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import lombok.*;
import lombok.experimental.Accessors;
import org.cactoos.io.InputOf;
import org.cactoos.scalar.UncheckedScalar;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Accessors(fluent = true)
@Getter
@Builder(toBuilder = true)
@ToString
public final class Project {

    private String artifactId;
    private String groupId;

    @Builder.Default
    private Iterable<Dep> deps = ImmutableList.of();

    private Path workDir;
    private Path pomParent;

    @Builder.Default
    private Path m2Home = getTmpDirectory();

    @Builder.Default
    private List<OutputFile> outputs = Lists.newArrayList();
    private Path baseImage;
    private ByteSource pomTemplate;
    @Builder.Default
    private Args args = new Args();

    private Path pom;


    public Path repository() {
        return this.m2Home().resolve("repository");
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
                if (pomParent != null) {
                    final Path pom = pom();
                    final Path relativize = pom.relativize(pomParent);
                    return relativize.toString();
                }
                return null;

            }
        };
    }


    public Pom.Props getPomProps() {
        return new UncheckedScalar<>(
                new Pom.XPath(
                        new InputOf(this.pom())
                )
        ).value();
    }

    public Path getArtifactFolder() {
        final Pom.Props bean = getPomProps();
        return new Dep.Simple(null,
                bean.getGroupId(),
                bean.getArtifactId(),
                bean.getVersion()
        ).relativeTo(repository());
    }

    interface ProjectView {
        Iterable<Dep> deps();
        String groupId();
        String artifactId();
        String parent();

    }

    public Path pom() {
        if (pom == null) {
            pom = syntheticPomFile(workDir);
        }
        return pom;
    }

    @SneakyThrows
    private static Path getTmpDirectory() {
        final String dirName = SysProps.labelHex().map(n -> n + "__M2_HOME@").orElse("M2_HOME@");
        return Files.createTempDirectory(dirName);
    }

    public static Path syntheticPomFile(Path workDir) {
        for (int i = 0; i < 1000; i++) {
            final Path pom = workDir.resolve(
                    RandomText.randomFileName("pom-synthetic") + "-" + i + ".xml"
            );
            if (Files.notExists(pom)) {
                return pom;
            }
        }
        throw new IllegalStateException();
    }

    public static String name(Path pom) {
        return SysProps.labelHex().orElse("");
    }
}
