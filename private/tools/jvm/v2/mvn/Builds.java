package tools.jvm.v2.mvn;

import com.google.gson.annotations.SerializedName;
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
    private final Map<String, BuildHolder> buildMap = new HashMap<>();


    /**
     * Builds order according to pom structure and deps.
     */
    interface BuildsOrder {
        void each(Consumer<Build> file);
    }

    /**
     * Build.
     */
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

        public BuildInfo(Path p) {
            this.file = p;
        }

        @SuppressWarnings("unused")
        public BuildInfo() {
            this.file = null;
            this.flags = null;
        }
    }

    /**
     * Add by file.
     * @param info file
     */
    public void registerFile(File info) {
        registerFile(new BuildInfo(info.toPath()));
    }

        /**
         * Add pom file.
         *
         * @param info pom file location
         */
    public void registerFile(BuildInfo info) {
        Optional<BuildHolder> current = Optional
                .of(new BuildHolder(info.file.toFile(), info.flags))
                .map(this::register);

        while (current.isPresent()) {
            current.ifPresent(holder -> {
                final Optional<BuildHolder> maybeParent = holder.pomFile().pom().relativePath().map(rel -> {
                    final Path parentFile = holder.originFile.toPath().getParent().resolve(rel).normalize();
                    return new BuildHolder(parentFile.toFile(), holder._args);
                }).map(this::register);

                if (holder._parent == null) {
                    holder._parent = maybeParent.orElse(null);
                }

                maybeParent.ifPresent(parent -> parent._children.add(holder));
            });
            current = current.flatMap(holder -> Optional.ofNullable(holder._parent));
        }
    }

    private BuildHolder register(BuildHolder h) {
        return buildMap.computeIfAbsent(h.id, (k) -> h);
    }

    /**
     * Get builds order.
     *
     * @return builds.
     */
    public BuildsOrder travers() {
        return new PreOrderBuilds(buildMap.values());
    }

    @ToString(of = {"id"})
    @EqualsAndHashCode(of = {"id"})
    private static class BuildHolder implements Build {
        private final String id;
        private File originFile;
        private PomFile origin;
        private Arg _args;

        private final Set<BuildHolder> _children = new LinkedHashSet<>();
        private BuildHolder _parent;

        private final PomFile wrap = new PomFile() {
            @Override
            public Pom pom() {
                return origin.pom();
            }

            @Override
            public PomFile update(PomUpdate... upd) {
                origin = origin.update(upd);
                return this;
            }

            @Override
            public File persisted(boolean w) {
                final File location = origin.persisted(w);
                BuildHolder.this._children.forEach(dep ->
                        dep.pomFile().update(
                                new PomUpdate.NewRelativeParent(location.getName())
                        )
                );
                return location;
            }
        };

        @Override
        public PomFile pomFile() {
            return wrap;
        }

        @Override
        public Optional<Arg> arg() {
            return Optional.ofNullable(_args);
        }


        private BuildHolder(File pomFile, List<String> arg) {
            this(pomFile, getKey(pomFile.toPath()), arg != null ? new Arg(arg) : null);
        }

        private BuildHolder(File pomFile, Arg arg) {
            this(pomFile, getKey(pomFile.toPath()), arg);
        }

        private BuildHolder(File pomFile, String id, Arg arg) {
            this.originFile = pomFile;
            this.origin = new PomFile.Simple(pomFile);
            this.id = id;
            this._args = arg;
        }
    }


    private static class PreOrderBuilds implements BuildsOrder {
        private Map<String, BuildHolder> lookup = new HashMap<>();

        private PreOrderBuilds(Iterable<BuildHolder> defFiles) {
            defFiles.forEach(def -> lookup.put(def.id, def));
            for (BuildHolder defFile : defFiles) {
                final BuildHolder thisNode = lookup.get(defFile.id);
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

        public void forEach(BiConsumer<Integer, BuildHolder> nodeConsumer) {
            final Iterator<BuildHolder> roots = lookup.values().stream()
                    .filter(node -> node._parent == null).iterator();
            forEachImpl(roots, 0, nodeConsumer);
        }

        private void forEachImpl(Iterator<BuildHolder> it, int dep, BiConsumer<Integer, BuildHolder> nodeConsumer) {
            while (it.hasNext()) {
                final BuildHolder node = it.next();
                nodeConsumer.accept(dep, node);

                if (!node._children.isEmpty()) {
                    final Iterator<BuildHolder> childIt = node._children.iterator();
                    forEachImpl(childIt, dep + 1, nodeConsumer);
                }
            }
        }

        @Override
        public String toString() {
            final StringWriter b = new StringWriter();
            forEach((idx, f) -> {
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
