package tools.jvm.mvn;

import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;

import java.nio.file.Files;
import java.nio.file.Path;

public interface Project {

    default String artifactId() {
        return RandomText.randomStr("artifact-id");
    }

    default String groupId() {
        return RandomText.randomStr("group-id");
    }

    default Iterable<Dep> deps() {
        return ImmutableList.of();
    }

    default Path workDir() {
        throw new IllegalStateException("not impl workDir");
    }

    @SneakyThrows
    default Path m2Home() {
        return Files.createTempDirectory("M2_HOME-");
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

    default Path pom() {
        for (int i = 0; i < 1000; i++) {
            final Path pom = workDir().resolve(RandomText.randomStr("pom_synthetic") + "-" + i + ".xml");
            if (Files.notExists(pom)) {
                return pom;
            }
        }
        throw new IllegalStateException();
    }

    /**
     * Memorized version of a Project.
     */
    default Project lazy() {
        return Memento.memorize(this);
    }


    default PropsView toView() {
        return new PropsView() {
            @Override
            public Iterable<Dep> deps() {
                return Project.this.deps();
            }

            @Override
            public String groupId() {
                return Project.this.groupId();
            }

            @Override
            public String artifactId() {
                return Project.this.artifactId();
            }
        };
    }

    interface PropsView {
        Iterable<Dep> deps();

        String groupId();

        String artifactId();
    }


    /**
     * Lazy memorized Project.
     */
    class Memorized extends Wrap implements Project {
        public Memorized(Project project) {
            super(Memento.memorize(project));
        }
    }


    @AllArgsConstructor
    @ToString
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
