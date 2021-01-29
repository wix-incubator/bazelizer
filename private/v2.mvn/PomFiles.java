package tools.jvm.v2.mvn;

import com.google.common.collect.Lists;
import lombok.Data;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PomFiles {
    private final Map<String, PomFileHolder> buildMap = new HashMap<>();

    public void addFile(Path file) {
        final PomFileHolder holder = buildMap.computeIfAbsent(getKey(file), (k) -> {
            PomFile f = new PomFile.Just(file.toFile());
            return new PomFileHolder(f);
        });
        walk(file, holder);
    }

    private void walk(Path orig, PomFileHolder current) {
        final Optional<Path> parentPath = current.pomFile.pom().relativePath().map(rel -> orig.relativize(Paths.get(rel)).normalize());
        parentPath.ifPresent(p -> {
            final PomFileHolder parentHolder = buildMap.computeIfAbsent(getKey(p), (k) -> {
                PomFile parent = new PomFile.Just(p.toFile());
                PomFileHolder parentBuild = new PomFileHolder(parent);
                parentBuild._children.add(current);
                current._parent = parentBuild;
                return parentBuild;
            });

            walk(p, parentHolder);
        });
    }

    private String getKey(Path p) {
        return p.toAbsolutePath().toString();
    }

    @Data
    private static class PomFileHolder {
        private final PomFile pomFile;

        private List<PomFileHolder> _children = Lists.newArrayList();
        private PomFileHolder _parent;
    }
}
