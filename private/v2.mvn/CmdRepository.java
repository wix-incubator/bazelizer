package tools.jvm.v2.mvn;

import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

public class CmdRepository implements Runnable {

    @CommandLine.Option(names = {"--def"})
    public Path pomDeclarations;

    @CommandLine.Option(names = {"-rs", "--mk-snapshot"}, paramLabel = "PATH")
    public Path writeRepository;

    @Override
    public void run() {
        final Mvn maven = new Mvn();

        final List<BuildNode> nodes = new Manifest(pomDeclarations).items(BuildNode.class);
        new PreOrderIterator(nodes).forEach(node -> {



        });
    }

    @Data
    private static class BuildNode {
        @SerializedName("file")
        private Path file;
        @SerializedName("parent_file")
        private Path parentFile;
        @SerializedName("flags_line")
        private List<String> flags;

        @Getter(AccessLevel.PRIVATE)
        private List<BuildNode> _children = Lists.newArrayList();

        @Getter(AccessLevel.PRIVATE)
        private BuildNode _parent;

        public String id() {
            return file.toString();
        }
        public String parentId() {
            return Optional.ofNullable(parentFile).map(Path::toString).orElse(null);
        }

//        public Args args() {
//            return Optional.ofNullable(flags)
//                    .filter(d -> !d.isEmpty())
//                    .map(flags -> {
//                        final String line = String.join(" ", flags);
//                        return new Args().parseCommandLine(line);
//                    }).orElse(new Args());
//        }
    }



    private static class PreOrderIterator {
        private Map<String, BuildNode> lookup = new HashMap<>();

        private PreOrderIterator(List<BuildNode> defFiles) {
            defFiles.forEach(def -> lookup.put(def.id(), def));
            for (BuildNode defFile : defFiles) {
                final BuildNode thisNode = lookup.get(defFile.id());
                if (thisNode._parent != null) {
                    lookup.computeIfPresent(thisNode.parentId(), (k, parent) -> {
                        parent._children.add(thisNode);
                        thisNode._parent = parent;
                        return parent;
                    });
                }
            }
        }

        public void forEach(Consumer<BuildNode> nodeConsumer) {
            final Iterator<BuildNode> roots = lookup.values().stream()
                    .filter(node -> node.parentId() == null).iterator();
            forEachImpl(roots, nodeConsumer);
        }

        private void forEachImpl(Iterator<BuildNode> it, Consumer<BuildNode> nodeConsumer) {
            while (it.hasNext()) {
                final BuildNode node = it.next();
                nodeConsumer.accept(node);

                if (!node._children.isEmpty()) {
                    final Iterator<BuildNode> childIt = node._children.iterator();
                    forEachImpl(childIt,nodeConsumer);
                }
            }
        }
    }

}
