package tools.jvm.mvn;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Suppliers;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.ToString;
import org.cactoos.iterable.Filtered;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public interface Builds extends Iterable<Builds.BuildNode> {



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
    
    class JsonManifestOf implements Builds {

        /**
         * Ctor
         * @param pomDeclarations manifest path
         */
        @SuppressWarnings("StaticPseudoFunctionalStyleMethod")
        public JsonManifestOf(Path pomDeclarations) {
            lazy = Suppliers.memoize(() -> {
                final List<DefPom> defFiles = Lists.newArrayList(
                        Iterables.transform(
                                new ManifestFile(pomDeclarations), DefPom::deserialize
                        )
                );
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
                return new Filtered<>(lookup.values(), v -> v.parent == null);
            });
        }

        /**
         * Lazy computed nodes.
         */
        private final Supplier<Iterable<BuildNode>> lazy;

        @Override
        public Iterator<BuildNode> iterator() {
            return lazy.get().iterator();
        }

        @Override
        public String toString() {
            final MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
            helper.add("parents=", Joiner.on(", ").join(this));
            return helper.toString();
        }
    }
    
}
