package tools.jvm.mvn;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.StringJoiner;

import static com.google.common.io.Files.asByteSource;

@AllArgsConstructor
@Slf4j
@SuppressWarnings({"UnstableApiUsage", "RedundantSuppression"})
public class ActAssemble implements Act {

    /**
     * Definition of all builds.
     */
    private final Iterable<Builds.PomDefinition> def;

    /**
     * Target action for each build.
     */
    private final Act act;


    @Override
    public Project accept(Project simple) {
        final Iterable<Builds.BuildNode> builds = new Builds.PreOrderGraph(def);

        log.info("Evaluate build graph:\n{}", builds);

        for (Builds.BuildNode node : builds) {
            final Builds.PomDefinition pom = node.self();
            final Path pomFile = pom.getFile();
            final Path parent = pomFile.getParent();

            final String id = SLF4JConfigurer.shortMDC(pomFile);
            SLF4JConfigurer.withMDC(id, () -> {

                final Project build = Project.builder()
                        .args(
                                new Args(simple.args())
                                        .merge(node.self().args())
                        )
                        .workDir(parent)
                        .pomTemplate(asByteSource(pomFile.toAbsolutePath().toFile()))
                        .parentPom(pom.getParentFile() != null ? pom.getParentFile().toAbsolutePath() : null)
                        .build();

                log.info("running build {}", node);

                act.accept(
                        build.toBuilder().m2Directory(simple.m2Directory()).build()
                );
            });
        }

        return simple;
    }


    private String mdc(Path pomFile) {
        final int count = pomFile.getNameCount();
        String s;
        if (count > 3) {
            s = "../" + pomFile.subpath(count - 2, count);
        } else {
            s = pomFile.toString();
        }
        return "[" + s + "]";
    }
}
