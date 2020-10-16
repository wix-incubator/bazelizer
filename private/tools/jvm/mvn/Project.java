package tools.jvm.mvn;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.Accessors;

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
    private List<OutputFile> outputs;
    private Path baseImage;
    private ByteSource pomXmlSrc;
    @Builder.Default
    private Args args = new Args();
    private Path pom;

    public Path repository() {
        return this.m2Home().resolve("repository");
    }

    ProjectView toView() {
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

    interface ProjectView {
        Iterable<Dep> deps();
        String groupId();
        String artifactId();
        String parent();

    }

    public Path pom() {
        if (pom == null) {
            pom = syntheticPom();
        }
        return pom;
    }

    @SneakyThrows
    private static Path getTmpDirectory() {
        return Files.createTempDirectory("M2_HOME@");
    }

    private Path syntheticPom() {
        for (int i = 0; i < 1000; i++) {
            final Path pom = this.workDir().resolve(
                    RandomText.randomFileName("pom_synthetic") + "-" + i + ".xml"
            );
            if (Files.notExists(pom)) {
                return pom;
            }
        }
        throw new IllegalStateException();
    }
}
