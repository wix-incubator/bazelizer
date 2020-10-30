package tools.jvm.mvn;

import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;
import org.cactoos.iterable.IterableOf;
import org.cactoos.iterable.Joined;
import org.cactoos.iterable.Mapped;
import org.xembly.Directive;
import org.xembly.Directives;

public interface XeSource {
    Iterable<Directive> value();


    /**
     * Join directives.
     * @param xe dirs
     * @return joined
     */
    static XeSource join(XeSource...xe) {
        return () -> new Joined<>(
                new Mapped<>(new IterableOf<>(xe), XeSource::value)
        );
    }

    @AllArgsConstructor
    static class All implements XeSource {
        @Delegate
        private final XeSource src;

        public All(XeSource...xe) {
            this(() -> new Joined<>(new Mapped<>(new IterableOf<>(xe), XeSource::value)));
        }
    }

    /**
     * Ensure <dependencies></Dependencies> tag exists.
     */
    class HasDependenciesTag implements XeSource {
        @Override
        public Iterable<Directive> value() {
            return new Directives()
                    .xpath("/project")
                    .addIf("dependencies");
        }
    }

    /**
     * Remove add dependency except special marker.
     */
    class RemoveDependencyTags implements XeSource {
        @Override
        public Iterable<Directive> value() {
            return new Directives()
                    .xpath("/project/dependencies/dependency[not(@xhtml:class=\"freeze\")]")
                    .remove();
        }
    }

    /**
     * Remove add dependency except special marker.
     */
    @AllArgsConstructor
    class AddDeps implements XeSource {

        private final Iterable<Dep> deps;

        @Override
        public Iterable<Directive> value() {
            final Directives directives = new Directives()
                    .xpath("/project/dependencies");

            deps.forEach(dep -> {
                directives.add("dependency");
                directives.add(
                        ImmutableMap.of(
                                "groupId", dep.groupId(),
                                "artifactId", dep.artifactId(),
                                "version", dep.version()
                        )
                );
                directives.up();
            });
            directives.up();
            return directives;

        }
    }
}
