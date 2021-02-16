package tools.jvm.v2.mvn;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import org.xembly.Directive;
import org.xembly.Directives;

import java.nio.file.Paths;
import java.util.*;

public interface PomUpdate {

    /**
     * Dirs.
     * @param xml pom
     * @return dirs.
     */
    Iterable<Directive> apply(Pom xml);

    /**
     * The PomStruc.
     */
    class PomStruc implements PomUpdate {
        @Override
        public Iterable<Directive> apply(Pom xml) {
            return new Directives().xpath("/project").addIf("dependencies");
        }
    }

    /**
     * The PomDropDeps.
     */
    class PomDropDeps implements PomUpdate {

        public static final String ATTR = "@" + Pom.NS + ":drop";


        public static final String SINGLE_DEP_EXCLUDE_MARKED = String.format(
                "/project/dependencies/dependency[not(%s=\"never\")]",
                ATTR
        );

        public static final String SINGLE_DEP_DROP_MARKED = String.format(
                "/project/dependencies/dependency[%s=\"true\"]",
                ATTR
        );

        public static final String DEPS_GET_ATTR = String.format(
                "/project/dependencies/%s",
                ATTR
        );

        @Override
        public Iterable<Directive> apply(Pom pom) {
            final List<String> attrs = pom.xml().xpath(DEPS_GET_ATTR);
            if (!attrs.isEmpty()) {
                final String s = attrs.get(0).trim().toLowerCase();
                if ("never".equals(s)) {
                    return new Directives()
                            .xpath(SINGLE_DEP_DROP_MARKED)
                            .remove();
                }
            }
            return new Directives()
                    .xpath(SINGLE_DEP_EXCLUDE_MARKED)
                    .remove();
        }
    }

    /**
     * The PomStruc.
     */
    @AllArgsConstructor
    class NewRelativeParent implements PomUpdate {
        private final String filename;

        @Override
        public Iterable<Directive> apply(Pom xml) {
            final Optional<String> aName = xml.relativePath()
                    .map(p -> Paths.get(p).getParent().resolve(filename).toString());
            return aName.map(s -> (Iterable<Directive>) new Directives()
                    .xpath("/project/parent/relativePath")
                    .set(s))
                    .orElseGet(Collections::emptyList);

        }
    }

    @AllArgsConstructor
    class AppendDeps implements PomUpdate {
        private final Iterable<Dep> deps;

        @Override
        public Iterable<Directive> apply(Pom xml) {
            if (Iterables.isEmpty(deps)) {
                return ImmutableList.of();
            }
            final Directives depsDir = new Directives()
                    .xpath("/project/dependencies");
            for (Dep dep : deps) {
                final Map<Object, Object> depProp = Maps.newLinkedHashMap();
                depProp.put("groupId", dep.getGroupId());
                depProp.put("artifactId", dep.getArtifactId());
                depProp.put("version", dep.getVersion());
                depProp.put("scope", dep.scope());

                depsDir.add("dependency")
                        .comment(" by: " + dep.getSource() + " ")
                        .add(depProp)
                        .up();
            }
            return depsDir;
        }
    }

}
