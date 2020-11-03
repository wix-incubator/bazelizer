package tools.jvm.mvn;

import com.google.common.collect.Maps;
import com.jcabi.xml.XML;
import org.xembly.Directive;
import org.xembly.Directives;

import java.util.HashMap;

public interface XemblyFunc {

    Iterable<Directive> dirs(Project project, XML xml);


    class PomDefaultStruc implements XemblyFunc {
        @Override
        public Iterable<Directive> dirs(Project project, XML xml) {
            return new Directives().xpath("/project").addIf("dependencies");
        }
    }

    class PomParentRelPath implements XemblyFunc {
        @Override
        public Iterable<Directive> dirs(Project project, XML xml) {
            return new Directives()
                    .xpath("/project/parent")
                    .addIf("relativePath").set(project.toView().parent());
        }
    }

    class PomDropDeps implements XemblyFunc {
        @Override
        public Iterable<Directive> dirs(Project project, XML xml) {
            return new Directives().xpath("/project/dependencies/dependency[not(@xe:remove=\"never\")]").remove();
        }
    }

    class AppendDeps implements XemblyFunc {
        @Override
        public Iterable<Directive> dirs(Project project, XML xml) {
            final Directives deps = new Directives()
                    .xpath("/project/dependencies");
            for (Dep dep : project.deps()) {
                final HashMap<String, String> map = Maps.newHashMap();
                map.put("groupId", dep.groupId());
                map.put("artifactId", dep.artifactId());
                map.put("version", dep.version());
                map.put("scope", dep.scope());
                deps.add(map);
            }
            return deps;
        }
    }
}
