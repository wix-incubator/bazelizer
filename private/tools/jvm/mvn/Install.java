package tools.jvm.mvn;

import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.cactoos.Proc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

public interface Install extends Proc<Path> {


    static Install all(Install...ii) {
        return new Install() {
            @Override
            public void exec(Path repo) throws Exception {
                for (Install install : ii) {
                    install.exec(repo);
                }
            }
        };
    }

    @AllArgsConstructor
    class Folder implements Install {
        private final Dep dep;

        @Override
        public void exec(Path input) throws Exception {
            final Path path = dep.artifactFolder(input);
            //noinspection ResultOfMethodCallIgnored
            path.toFile().mkdirs();
        }
    }

    @AllArgsConstructor
    @ToString
    @Slf4j
    class NewPom implements Install {
        private final Dep dep;

        @Override
        public void exec(Path repo) throws Exception {
            final Path artifactFolder = dep.artifactFolder(repo);
            String fileName = dep.artifactId() + "-" + dep.version();
            Path pomFile = artifactFolder.resolve(fileName + ".pom");
            String pom = "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://maven.apache.org/POM/4.0.0\" " +
                    "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                    "<modelVersion>4.0.0</modelVersion>\n" +
                    "<groupId>" + dep.groupId() + "</groupId>\n" +
                    "<artifactId>" + dep.artifactId() + "</artifactId>\n" +
                    "<version>" + dep.version() + "</version>\n" +
                    "<description>Generated by " + this.getClass() + " for " + dep + "</description>\n" +
                    "</project>";
            Files.write(pomFile, pom.getBytes(StandardCharsets.UTF_8));
            log.debug("write pom {}\n{}", pomFile, pom);
        }
    }

    @AllArgsConstructor
    @ToString
    class NewJar implements Install {
        private final Dep dep;

        @Override
        public void exec(Path repo) throws Exception {
            Path jarFile = dep.artifactFolder(repo).resolve(dep.artifactId() + "-" + dep.version() + ".jar");
            Files.copy(dep.source(), jarFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @AllArgsConstructor
    @ToString
    class Untar implements Install {
        private final Dep dep;

        @Override
        public void exec(Path repo) throws Exception {
            final Path tarSrc = dep.source();
            Archive.extractTar(tarSrc, repo);
        }
    }
}
