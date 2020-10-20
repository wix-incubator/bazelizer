package tools.jvm.mvn;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Delegate;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    class PomDefs implements Iterable<DefPom> {
        @Delegate
        private final Iterable<DefPom> iter;

        @SuppressWarnings("StaticPseudoFunctionalStyleMethod")
        public PomDefs(Path pomDefFile) {
            this.iter = Lists.newArrayList(
                    Iterables.transform(
                            new ManifestFile(pomDefFile), DefPom::deserialize
                    )
            );
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
                            Streams.concat(
                                Stream.of(node),
                                node.getChildren().stream()
                        )
                    ).collect(Collectors.toList());
        }
    }
    
}
