package tools.jvm.mvn;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.io.Files.asByteSource;

@SuppressWarnings("UnstableApiUsage")
@AllArgsConstructor
public class Projects implements Act {

    private final Path def;

    private final Act act;

    @Override
    public Project accept(Project simple) {
        for (Builds.BuildNode node : new Builds.JsonManifestOf(def)) {
            final List<Project> projects = projectOf(node);
            for (Project project : projects) {
                act.accept(
                        project.toBuilder()
                                .m2Home(simple.m2Home())
                                .build()
                );
            }
        }
        return simple;
    }

    public List<Project> projectOf(Builds.BuildNode node) {
        final Builds.DefPom pom = node.getSelf();

        final Path pomFile = pom.getFile();
        final Path parent = pomFile.getParent();

        final Project build = Project.builder()
                .workDir(parent)
                .pomTpl(asByteSource(pomFile.toAbsolutePath().toFile()))
                .pomParent(pom.getParentFile() != null ? pom.getParentFile().toAbsolutePath() : null)
                .build();

        final ArrayList<Project> res = Lists.newArrayList();
        res.add(build);
        for (Builds.BuildNode aNode : node.getChildren()) {
            res.addAll(projectOf(aNode));
        }
        return res;
    }
}
