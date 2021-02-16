package tools.jvm.v2.mvn;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
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
    private final Map<String, BuildImpl> buildMap = new HashMap<>();


    /**
     * Builds order according to pom structure and deps.
     */
    interface BuildsOrder {
        void each(Consumer<Build> file);
    }


    public interface Build {
         PomFile pomFile();
         Optional<Arg> arg();
    }

    @Data
    public static class BuildInfo {
        @SerializedName("file")
        private Path file;
        @SerializedName("flags_line")
        private List<String> flags;
    }

    /**
     * Add pom file.
     * @param info pom file location
     */
    public void registerFile(BuildInfo info) {
        final Path file = info.file.toAbsolutePath();
        final BuildImpl holder = buildMap.compute(getKey(file), (k, maybeBuild) -> {
            final BuildImpl build;
            if (maybeBuild == null) {
                build = new BuildImpl(new PomFile.Simple(file.toFile()), k);
            } else {
                build = maybeBuild;
            }
            if (build._args == null && info.flags != null) {
                build._args = new Arg(info.flags);
            }
            return build;
        });
        System.out.println(file);
        walk(file, holder, buildMap);
    }

    /**
     * Get builds order.
     * @return builds.
     */
    public BuildsOrder travers() {
        return new PreOrderBuilds(buildMap.values());
    }


    private static void walk(Path orig, BuildImpl current, Map<String, BuildImpl> buildMap) {
        final Optional<Path> parentPath = current.origin.pom()
                .relativePath().map(rel -> orig.getParent().resolve(rel).normalize());
        parentPath.ifPresent(p -> {
            final BuildImpl parentHolder = buildMap.computeIfAbsent(getKey(p), (k) -> {
                PomFile parent = new PomFile.Simple(p.toFile());
                BuildImpl parentBuild = new BuildImpl(parent, k);
                parentBuild._children.add(current);
                current._parent = parentBuild;
                return parentBuild;
            });

            walk(p, parentHolder, buildMap);
        });
    }


    @ToString(of = {"id"})
    @EqualsAndHashCode(of = {"id"})
    private static class BuildImpl implements Build {
        private final String id;
        private final PomFile origin;
        private Arg _args;

        @AllArgsConstructor
        private class WrapFile implements PomFile {
            private PomFile file;

            @Override
            public Pom pom() {
                return file.pom();
            }

            @Override
            public PomFile update(PomUpdate... upd) {
                file = file.update(upd);
                return this;
            }

            @Override
            public File persisted(boolean w) {
                final File location = file.persisted(w);
                BuildImpl.this._children.forEach(dep ->
                        dep.pomFile().update(new PomUpdate.NewRelativeParent(location.getName()))
                );
                return location;
            }
        }

        @Override
        public PomFile pomFile() {
            return new BuildImpl.WrapFile(this.origin);
        }

        @Override
        public Optional<Arg> arg() {
            return Optional.ofNullable(_args);
        }


        private BuildImpl(PomFile pomFile, String id) {
            this.origin = pomFile;
            this.id = id;
        }

        private final Set<BuildImpl> _children = new LinkedHashSet<>();
        private BuildImpl _parent;
    }


    private static class PreOrderBuilds implements BuildsOrder {
        private Map<String, BuildImpl> lookup = new HashMap<>();

        private PreOrderBuilds(Iterable<BuildImpl> defFiles) {
            defFiles.forEach(def -> lookup.put(def.id, def));
            for (BuildImpl defFile : defFiles) {
                final BuildImpl thisNode = lookup.get(defFile.id);
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
        public void each(Consumer<Build> file) {
            forEach((x, holder) -> file.accept(holder));
        }

        public void forEach(BiConsumer<Integer, BuildImpl> nodeConsumer) {
            final Iterator<BuildImpl> roots = lookup.values().stream()
                    .filter(node -> node._parent == null).iterator();
            forEachImpl(roots, 0, nodeConsumer);
        }

        private void forEachImpl(Iterator<BuildImpl> it, int dep, BiConsumer<Integer, BuildImpl> nodeConsumer) {
            while (it.hasNext()) {
                final BuildImpl node = it.next();
                nodeConsumer.accept(dep, node);

                if (!node._children.isEmpty()) {
                    final Iterator<BuildImpl> childIt = node._children.iterator();
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

    private static String getKey(Path p) {
        return p.toAbsolutePath().toString();
    }

}
