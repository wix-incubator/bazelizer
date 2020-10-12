package tools.jvm.mvn;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.io.ByteSource;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

@Accessors(fluent = true, chain = false)
@Getter
@Builder(toBuilder = true)
public final class Project {
    private String artifactId;
    private String groupId;
    @Builder.Default
    private Iterable<Dep> deps = ImmutableList.of();
    private Path workDir;
    @Builder.Default
    private Path m2Home = getTmpDirectory();
    private Iterable<Output> outputs;
    private Path baseImage;
    private ByteSource pomXmlSrc;
    @Builder.Default
    private Args args = new Args();
    private Path pomDest;

    PropsView toView() {
        return new PropsView() {
            @Override
            public Iterable<Dep> deps() {
                return () -> new UnmodifiableIterator<Dep>() {
                    final Iterator<Dep> it = Project.this.deps.iterator();
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }
                    @Override
                    public Dep next() {
                        return it.next();
                    }
                };
            }

            @Override
            public String groupId() {
                return groupId;
            }

            @Override
            public String artifactId() {
                return artifactId;
            }
        };
    }

    interface PropsView {
        Iterable<Dep> deps();
        String groupId();
        String artifactId();

    }

    public Path pomDest() {
        if (pomDest == null) {
            pomDest = syntheticPom();
        }
        return pomDest;
    }

    @SneakyThrows
    private static Path getTmpDirectory() {
        return Files.createTempDirectory("M2_HOME@");
    }

    private Path syntheticPom() {
        for (int i = 0; i < 1000; i++) {
            final Path pom = this.workDir().resolve(RandomText.randomStr("pom_synthetic") + "-" + i + ".xml");
            if (Files.notExists(pom)) {
                return pom;
            }
        }
        throw new IllegalStateException();
    }
}
