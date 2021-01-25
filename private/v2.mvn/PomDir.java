package tools.jvm.v2.mvn;

import com.jcabi.xml.XML;
import org.xembly.Directive;
import org.xembly.Directives;

public interface PomDir {
    Iterable<Directive> dirs(XML xml);


    class PomStruc implements PomDir {
        @Override
        public Iterable<Directive> dirs(XML xml) {
            return new Directives().xpath("/project").addIf("dependencies");
        }
    }

    class PomDropDeps implements PomDir {
        @Override
        public Iterable<Directive> dirs( XML xml) {
            return new Directives()
                    .xpath("/project/dependencies/dependency[not(@bz:remove=\"never\")]")
                    .remove();
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
