package tools.jvm.mvn;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.io.ByteSource;
import com.google.common.reflect.AbstractInvocationHandler;
import com.google.common.reflect.Invokable;
import com.google.common.reflect.Reflection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@SuppressWarnings("NullableProblems")
public interface Project {

    default String artifactId() {
        return RandomTextUtil.randomStr("artifact-id");
    }

    default String groupId() {
        return RandomTextUtil.randomStr("group-id");
    }

    default Iterable<Dep> deps() {
        return ImmutableList.of();
    }

    default Path workDir() {
        throw new IllegalStateException("not impl workDir");
    }

    @SneakyThrows
    default Path m2Home() {
        return Files.createTempDirectory("M2_HOME");
    }

    default Iterable<Output> getOutputs() {
        return ImmutableList.of();
    }

    default Path repoImage() {
        throw new IllegalStateException();
    }

    default ByteSource pomXmlTpl() {
        throw new IllegalStateException();
    }

    default Args args() {
        return new Args();
    }

    /**
     * Output.
     */
    interface Output {
        String src();
        String dest();
    }

    @Data
    class OutputPaths implements Output {
        private final String src;
        private final String dest;

        @Override
        public String src() {
            return src;
        }

        @Override
        public String dest() {
            return dest;
        }
    }

    class TmpSrc implements Output {
        private final Path dest;
        private final Path src;

        @SneakyThrows
        public TmpSrc(Path dest) {
            this.dest = dest;
            this.src = Files.createTempFile("tmp-src", ".dat");
        }

        @Override
        public String src() {
            return src.toString();
        }

        @Override
        public String dest() {
            return dest.toString();
        }
    }


    @SuppressWarnings({"UnstableApiUsage"})
    static Project memento(Project self) {
        final Map<Method,Object> cache = Maps.newHashMap();
        return Reflection.newProxy(Project.class, new AbstractInvocationHandler() {

            @Override
            protected Object handleInvocation(Object o, Method method, Object[] args) {
                return cache.computeIfAbsent(method, (m) -> call(args, m));
            }

            @SuppressWarnings("unchecked")
            private Object call(Object[] args, Method m)  {
                final Invokable<Project, Object> invokable = (Invokable<Project, Object>) Invokable.from(m);
                try {
                    return invokable.invoke(self, args);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new MvnException(e);
                }
            }
        });
    }


    @AllArgsConstructor
    class Wrap implements Project {
        private final Project project;

        @Override
        public String artifactId() {
            return project.artifactId();
        }

        @Override
        public String groupId() {
            return project.groupId();
        }

        @Override
        public Iterable<Dep> deps() {
            return project.deps();
        }

        @Override
        public Path workDir() {
            return project.workDir();
        }

        @Override
        @SneakyThrows
        public Path m2Home() {
            return project.m2Home();
        }

        @Override
        public Iterable<Output> getOutputs() {
            return project.getOutputs();
        }

        @Override
        public Path repoImage() {
            return project.repoImage();
        }

        @Override
        public ByteSource pomXmlTpl() {
            return project.pomXmlTpl();
        }

        @Override
        public Args args() {
            return project.args();
        }
    }
}
