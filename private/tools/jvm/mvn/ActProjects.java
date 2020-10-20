package tools.jvm.mvn;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.io.Files.asByteSource;

@SuppressWarnings("UnstableApiUsage")
@AllArgsConstructor
public class ActProjects implements Act {

    /**
     * Definition of all builds.
     */
    private final Path def;

    /**
     * Target action for each build.
     */
    private final Act act;


    @Override
    public Project accept(Project simple) {
        final Iterable<Builds.BuildNode> builds = new Builds.PreOrderGraph(
                new Builds.PomDefs(def)
        );

        for (Builds.BuildNode node : builds) {
            final Builds.DefPom pom = node.getSelf();
            final Path pomFile = pom.getFile();
            final Path parent = pomFile.getParent();

            final Project build = Project.builder()
                    .workDir(parent)
                    .pomTemplate(asByteSource(pomFile.toAbsolutePath().toFile()))
                    .pomParent(pom.getParentFile() != null ? pom.getParentFile().toAbsolutePath() : null)
                    .build();

            act.accept(
                    build.toBuilder().m2Home(simple.m2Home()).build()
            );
        }

        return simple;
    }
}
