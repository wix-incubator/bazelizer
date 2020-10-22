package tools.jvm.mvn;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.*;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Delegate;
import lombok.var;

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
    class DefPom {
        @SerializedName("file")
        private Path file;
        @SerializedName("parent_file")
        private Path parentFile;

        @SuppressWarnings("unused")
        public DefPom() {}

        public DefPom(Path path, Path parentFile) {
            this.file = path;
            this.parentFile = parentFile;
        }

        public DefPom(String path, String parentFile) {
            this(Paths.get(path), Optional.ofNullable(parentFile).map(Paths::get).orElse(null));
        }

        public String id() {
            return file.toString();
        }

        public String parentId() {
            return parentFile != null ? parentFile.toString() : null;
        }

        /**
         * Construct from JSON
         * @param line json
         * @return def
         */
        static DefPom deserialize(String line) {
            return tools.jvm.mvn.Deps.GSON.fromJson(line, DefPom.class);
        }
    }
    
    /**
     *  Build execution node
     */
    @Data
    @ToString(of = {"self"})
    class BuildNode {
        private final DefPom self;
        private List<BuildNode> children = Lists.newArrayList();
        private BuildNode parent;
    }


    /**
     * Source of pom definitions
     */
    class DefPomIterable implements Iterable<DefPom> {

        @Delegate
        private final Iterable<DefPom> iter;

        @SuppressWarnings("StaticPseudoFunctionalStyleMethod")
        public DefPomIterable(Path pomDefFile) {
            this.iter = new Iterable<DefPom>() {

                @SuppressWarnings("Guava")
                final Supplier<Iterable<String>> mem = Suppliers.memoize(() ->
                        Lists.newArrayList(new ManifestFile(pomDefFile)));

                @SuppressWarnings("NullableProblems")
                @Override
                public Iterator<DefPom> iterator() {
                    return Iterables.transform(mem.get(), DefPom::deserialize).iterator();
                }
            };
        }
    }


    /**
     * Pre order traversal of a graph.
     */
    @AllArgsConstructor
    class PreOrderGraph implements Iterable<BuildNode> {

        private final Iterable<DefPom> defPoms;

        @SuppressWarnings("NullableProblems")
        @Override
        public Iterator<BuildNode> iterator() {
            return preOrder().iterator();
        }

        @SuppressWarnings("UnstableApiUsage")
        private List<BuildNode> preOrder() {
            final ArrayList<DefPom> defFiles = Lists.newArrayList(this.defPoms);
            Map<String, BuildNode> lookup = Maps.newHashMap();
            defFiles.forEach(def -> lookup.put(def.id(), new BuildNode(def)));
            for (DefPom defFile : defFiles) {
                final BuildNode thisNode = lookup.computeIfAbsent(defFile.id(), i -> new BuildNode(defFile));
                if (thisNode.self.parentFile != null) {
                    lookup.computeIfPresent(thisNode.self.parentId(), (k, parent) -> {
                        parent.children.add(thisNode);
                        thisNode.parent = parent;
                        return parent;
                    });
                }
            }

            return lookup.values().stream().filter(node -> node.getParent() == null)
                    .flatMap(node ->
                            Streams.concat(Stream.of(node), node.getChildren().stream()
                        )
                    ).collect(Collectors.toList());
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        public String toString() {
            final Iterator<BuildNode> iterator = Iterators.filter(this.iterator(), v -> v.getParent() == null);
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
                    b.append("+- ");
                } else {
                    b.append("|");
                }
                b.append(" ");
                b.println(next.toString());

                if (!next.getChildren().isEmpty()) {
                    final Iterator<BuildNode> nextIt = next.getChildren().iterator();
                    while (nextIt.hasNext()) {
                        print(nextIt, tab+1, b);
                    }
                }
            }
        }
    }
}
