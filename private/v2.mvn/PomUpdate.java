package tools.jvm.v2.mvn;

import lombok.AllArgsConstructor;
import org.xembly.Directive;
import org.xembly.Directives;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

public interface PomUpdate {
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
        @Override
        public Iterable<Directive> apply(Pom xml) {
            return new Directives()
                    .xpath("/project/dependencies/dependency[not(@bz:remove=\"never\")]")
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
            return null;
        }
    }

//
//    class AppendDeps implements XemblyDirrective {
//        @Override
//        public Iterable<Directive> dirs(Project project, XML xml) {
//            final Iterable<Dep> deps = project.deps();
//            if (Iterables.isEmpty(deps)) {
//                return ImmutableList.of();
//            }
//
//            final Directives depsDir = new Directives()
//                    .xpath("/project/dependencies");
//            for (Dep dep : deps) {
//                final Map<Object, Object> depProp = Maps.newLinkedHashMap();
//                depProp.put("groupId", dep.groupId());
//                depProp.put("artifactId", dep.artifactId());
//                depProp.put("version", dep.version());
//                depProp.put("scope", dep.scope());
//
//                depsDir.add("dependency")
//                        .comment(" by: " + dep.source() + " ")
//                        .add(depProp)
//                        .up();
//            }
//            return depsDir;
//        }
//    }
}
