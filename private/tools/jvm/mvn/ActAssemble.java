package tools.jvm.mvn;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.io.Files.asByteSource;

@AllArgsConstructor
@Slf4j
public class ActAssemble implements Act {

    /**
     * Definition of all builds.
     */
    private final Iterable<Builds.DefPom> def;

    /**
     * Target action for each build.
     */
    private final Act act;


    @Override
    public Project accept(Project simple) {
        final Iterable<Builds.BuildNode> builds = new Builds.PreOrderGraph(def);

        for (Builds.BuildNode node : builds) {
            final Builds.DefPom pom = node.getSelf();
            final Path pomFile = pom.getFile();
            final Path parent = pomFile.getParent();

            try (MDC.MDCCloseable m = MDC.putCloseable("id", mdc(pomFile))) {
                @SuppressWarnings("UnstableApiUsage")
                final Project build = Project.builder()
                        .workDir(parent)
                        .pomTemplate(asByteSource(pomFile.toAbsolutePath().toFile()))
                        .pomParent(pom.getParentFile() != null ? pom.getParentFile().toAbsolutePath() : null)
                        .build();

                log.info("Evaluate build {}", node);

                act.accept(
                        build.toBuilder().m2Home(simple.m2Home()).build()
                );
            }
        }

        return simple;
    }

    private String mdc(Path pomFile) {
        final int count = pomFile.getNameCount();
        String s;
        if (count > 3) {
            s = "../" + pomFile.subpath(count - 2, count);
            return s;
        } else {
            s = pomFile.toString();
        }
        return "[" + s + "]";
    }
}
