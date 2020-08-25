package tools.jvm.mvn;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import lombok.AllArgsConstructor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Collection of a paths.
 *
 */
public interface PathsCollection extends Iterable<Path> {

    /**
     * New stream of paths
     * @return stream of paths
     */
    default Stream<Path> stream() {
        return Streams.stream(this);
    }

    /**
     * Get only one file as this files represent it or fail.
     * @return file.
     */
    default Path onlyOne() {
        return Iterables.getOnlyElement(this);
    }

    /**
     * Try to find common prefix for all paths.
     * @return common prefix path
     */
    default Path commonPrefix() {
        final Path first = Iterables.getFirst(this, null);
        if (first == null) {
            throw new IllegalStateException("empty");
        }

        BiPredicate<Path, Integer> matches = (path, i) -> {
            if (path == null) return false;
            if (i >= path.getNameCount()) return false;
            if (i >= first.getNameCount()) return false;
            if (i == 0) return first.getName(i).equals(path.getName(i));
            final Path path0 = first.subpath(0, i);
            final Path path1 = path.subpath(0, i);
            return path0.startsWith(path1);
        };

        return this.stream().map(Path::getNameCount).max(Comparator.naturalOrder()).flatMap(max -> {
            for (int x = 1; x <= max; x++) {
                final int i = x;
                if (!stream().allMatch(p -> matches.test(p, i))) {
                    return Optional.of(first.subpath(0, i-1));
                }
            }
            return Optional.empty();
        }).orElseThrow(() -> new IllegalStateException("no common path prefix within " + this));
    }

    /**
     * Read source from a manifest file. Expect each line a a source path.
     * @param file file
     * @return sources
     */
    static PathsCollection fromManifest(File file) {
        return new Manifest(file);
    }


    /**
     * Paths based on manifest file.
     */
    @SuppressWarnings("UnstableApiUsage")
    class Manifest implements PathsCollection {
        public static final String SUFFIX = "'";
        private final Collection<Path> paths;

        public Manifest(File man) {
            this(Files.asCharSource(man, StandardCharsets.UTF_8));
        }

        @lombok.SneakyThrows
        public Manifest(CharSource source) {
            this.paths = source
                    .readLines().stream()
                    .map(p -> {
                        String base = p.trim();
                        if (base.startsWith(SUFFIX)) {
                            base = base.substring(1);
                        }
                        if (base.endsWith(SUFFIX)) {
                            base = base.substring(0, base.length() - 1);
                        }
                        return base;
                    })
                    .map(p -> Paths.get(p))
                    .collect(Collectors.toSet());
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public Iterator<Path> iterator() {
            return paths.iterator();
        }

        @Override
        public String toString() {
            MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this);
            this.forEach(helper::addValue);
            return helper.toString();
        }
    }
}
