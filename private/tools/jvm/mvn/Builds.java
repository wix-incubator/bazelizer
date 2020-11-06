package tools.jvm.mvn;

import com.google.common.base.MoreObjects;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.*;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.Delegate;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Builds extends Iterable<Builds.BuildNode> {


    /**
     * Pom definition.
     */
    @Data
    class PomDefinition {
        @SerializedName("file")
        private Path file;
        @SerializedName("parent_file")
        private Path parentFile;
        @SerializedName("flags_line")
        private List<String> flags;

        @SuppressWarnings("unused")
        public PomDefinition() {
            super();
        }

        public PomDefinition(Path path, Path parentFile) {
            this.file = path;
            this.parentFile = parentFile;
        }

        public PomDefinition(String path, String parentFile) {
            this(Paths.get(path), Optional.ofNullable(parentFile).map(Paths::get).orElse(null));
        }

        /**
         * Current id
         * @return id
         */
        public String id() {
            return file.toString();
        }

        /**
         * id of parent node
         * @return id
         */
        public String parentId() {
            return parentFile != null ? parentFile.toString() : null;
        }

        /**
         * Command line args, specific for current build execution
         * @return args
         */
        public Args args() {
            return Optional.ofNullable(flags)
                    .filter(d -> !d.isEmpty())
                    .map(flags -> {
                        final String line = String.join(" ", flags);
                        return new Args().parseCommandLine(line);
                    }).orElse(new Args());
        }

        /**
         * Construct from JSON
         * @param line json
         * @return def
         */
        static PomDefinition deserialize(String line) {
            return tools.jvm.mvn.Deps.GSON.fromJson(line, PomDefinition.class);
        }
    }
    
    /**
     *  Build execution node
     */
    @Data
    @Accessors(fluent = true)
    class BuildNode {
        private final PomDefinition self;
        private List<BuildNode> children = Lists.newArrayList();
        private BuildNode parent;
        @Override
        public String toString() {
            return MoreObjects.toStringHelper("BuildNode")
                    .add("file", self.file)
                    .add("parent", self.parentFile)
                    .toString();
        }
    }


    /**
     * Source of pom definitions
     */
    class PomDefinitions implements Iterable<PomDefinition> {

        @Delegate
        private final Iterable<PomDefinition> iter;

        @SuppressWarnings("StaticPseudoFunctionalStyleMethod")
        public PomDefinitions(Path file) {
            this.iter = new Iterable<PomDefinition>() {
                @SuppressWarnings("Guava")
                final Supplier<Iterable<String>> mem = Suppliers.memoize(() ->
                        Lists.newArrayList(new ManifestFile(file)));

                @SuppressWarnings("NullableProblems")
                @Override
                public Iterator<PomDefinition> iterator() {
                    return Iterables.transform(mem.get(), PomDefinition::deserialize).iterator();
                }
            };
        }
    }


    /**
     * Pre order traversal of a graph.
     */
    @AllArgsConstructor
    class PreOrderGraph implements Iterable<BuildNode> {

        private final Iterable<PomDefinition> defPoms;

        @SuppressWarnings("NullableProblems")
        @Override
        public Iterator<BuildNode> iterator() {
            return preOrder().iterator();
        }

        @SuppressWarnings({"UnstableApiUsage", "RedundantSuppression"})
        private List<BuildNode> preOrder() {
            final ArrayList<PomDefinition> defFiles = Lists.newArrayList(this.defPoms);
            Map<String, BuildNode> lookup = Maps.newHashMap();
            defFiles.forEach(def -> lookup.put(def.id(), new BuildNode(def)));
            for (PomDefinition defFile : defFiles) {
                final BuildNode thisNode = lookup.computeIfAbsent(defFile.id(), i -> new BuildNode(defFile));
                if (thisNode.self.parentFile != null) {
                    lookup.computeIfPresent(thisNode.self.parentId(), (k, parent) -> {
                        parent.children.add(thisNode);
                        thisNode.parent = parent;
                        return parent;
                    });
                }
            }

            return lookup.values().stream()
                    .filter(node -> node.parent() == null)
                    .flatMap(node -> Streams.concat(Stream.of(node), node.children().stream()))
                    .collect(Collectors.toList());
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        public String toString() {
            final Iterator<BuildNode> iterator = Iterators.filter(this.iterator(), v -> v.parent() == null);
            final StringWriter out = new StringWriter();
            print(iterator, 0, new PrintWriter(out));
            return out.toString();
        }

        private void print(Iterator<BuildNode> it, int tab, PrintWriter b) {
            while (it.hasNext()) {
                final BuildNode next = it.next();
                for (int i = 0; i <= tab; i++) {
                    b.append("\t");
                }
                if (tab > 0) {
                    b.append("+-");
                } else {
                    b.append("|");
                }
                b.append(" ");
                b.println(next.toString());

                if (!next.children().isEmpty()) {
                    final Iterator<BuildNode> nextIt = next.children().iterator();
                    while (nextIt.hasNext()) {
                        print(nextIt, tab+1, b);
                    }
                }
            }
        }
    }
}
