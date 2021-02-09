package tools.jvm.v2.mvn;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.File;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Builds {
    private final Map<String, PomFileHolder> buildMap = new HashMap<>();


    /**
     * Builds order according to pom structure and deps.
     */
    interface BuildsOrder {
        void each(Consumer<PomFile> file);
    }


    /**
     * Add pom file.
     * @param path pom file location
     */
    public void registerFile(Path path) {
        final Path file = path.toAbsolutePath();
        final PomFileHolder holder = buildMap.computeIfAbsent(getKey(file), (k) -> {
            PomFile f = new PomFile.Simple(file.toFile());
            return new PomFileHolder(f, k);
        });
        System.out.println(file);
        walk(file, holder, buildMap);
    }

    /**
     * Get builds order.
     * @return builds.
     */
    public BuildsOrder builds() {
        return new PreOrderBuilds(buildMap.values());
    }


    private static void walk(Path orig, PomFileHolder current,  Map<String, PomFileHolder> buildMap) {
        final Optional<Path> parentPath = current.pomFile.pom()
                .relativePath().map(rel -> orig.getParent().resolve(rel).normalize());
        parentPath.ifPresent(p -> {
            final PomFileHolder parentHolder = buildMap.computeIfAbsent(getKey(p), (k) -> {
                PomFile parent = new PomFile.Simple(p.toFile());
                PomFileHolder parentBuild = new PomFileHolder(parent, k);
                parentBuild._children.add(current);
                current._parent = parentBuild;
                return parentBuild;
            });

            walk(p, parentHolder, buildMap);
        });
    }

    private static String getKey(Path p) {
        return p.toAbsolutePath().toString();
    }

    @Data
    private static class PomFileWrap implements PomFile {
        private final PomFileHolder h;



        @Override
        public Pom pom() {
            return h.pomFile.pom();
        }

        @Override
        public PomFile update(PomUpdate... upd) {
            return h.pomFile.update(upd);
        }

        @Override
        public File persisted(boolean w) {
            final File location = h.pomFile.persisted(w);
            h._children.forEach(dep -> {
                dep.pomFile.update(new PomUpdate.NewRelativeParent(location.getName()));
            });
            return location;
        }
    }

    @ToString(of = {"id"})
    @EqualsAndHashCode(of = {"id"})
    private static class PomFileHolder {
        private final PomFile pomFile;
        private final String id;

        private PomFileHolder(PomFile pomFile, String id) {
            this.pomFile = pomFile;
            this.id = id;
        }

        private final Set<PomFileHolder> _children = new LinkedHashSet<>();
        private PomFileHolder _parent;
    }


    private static class PreOrderBuilds implements BuildsOrder {
        private Map<String, PomFileHolder> lookup = new HashMap<>();

        private PreOrderBuilds(Iterable<PomFileHolder> defFiles) {
            defFiles.forEach(def -> lookup.put(def.id, def));
            for (PomFileHolder defFile : defFiles) {
                final PomFileHolder thisNode = lookup.get(defFile.id);
                if (thisNode._parent != null) {
                    final String parentId = thisNode._parent.id;
                    lookup.computeIfPresent(parentId, (k, parent) -> {
                        parent._children.add(thisNode);
                        thisNode._parent = parent;
                        return parent;
                    });
                }
            }
        }

        @Override
        public void each(Consumer<PomFile> file) {
            forEach((x,holder) -> file.accept(new PomFileWrap(holder)));
        }

        public void forEach(BiConsumer<Integer, PomFileHolder> nodeConsumer) {
            final Iterator<PomFileHolder> roots = lookup.values().stream()
                    .filter(node -> node._parent == null).iterator();
            forEachImpl(roots, 0, nodeConsumer);
        }

        private void forEachImpl(Iterator<PomFileHolder> it, int dep, BiConsumer<Integer, PomFileHolder> nodeConsumer) {
            while (it.hasNext()) {
                final PomFileHolder node = it.next();
                nodeConsumer.accept(dep, node);

                if (!node._children.isEmpty()) {
                    final Iterator<PomFileHolder> childIt = node._children.iterator();
                    forEachImpl(childIt, dep+1, nodeConsumer);
                }
            }
        }

        @Override
        public String toString() {
            final StringWriter b = new StringWriter();
            forEach((idx,f) -> {
                if (idx > 0) {
                    for (int i = idx; i > 0; i--) {
                        b.append("\t");
                    }
                    b.append("+-");
                } else {
                    b.append("|-");
                }
                b.append(" ").append(f.id).append("\n");
            });
            return b.toString();
        }
    }
}
