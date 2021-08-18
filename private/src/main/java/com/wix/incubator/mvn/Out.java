package com.wix.incubator.mvn;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

import static com.wix.incubator.mvn.IOSupport.REPOSITORY_FILES_FILTER;

public abstract class Out {

    /**
     * Save data.
     * @param maven curent env
     * @param pom current build
     */
    public abstract void save(Maven maven, Project.PomFile pom);

    @AllArgsConstructor
    public static class Jar extends Out {
        private final Path jarOutput;

        @SneakyThrows
        public void save(Maven maven, Project.PomFile pom) {
            final Path target = pom.target();
            final Path jar = target.resolve(String.format("%s-%s.jar", pom.model.getArtifactId(), pom.model.getVersion()));
            Files.copy(jar, jarOutput);
        }
    }


    @AllArgsConstructor
    public static class Installed extends Out {
        private final Path archive;

        @SneakyThrows
        public void save(Maven maven, Project.PomFile pom) {
            final String groupId = pom.model.getGroupId() == null ? pom.model.getParent().getGroupId() : pom.model.getGroupId();
            final Path installedFolder = Maven.artifactRepositoryLayout(groupId, pom.model.getArtifactId(), pom.model.getVersion());
            Collection<Path> files = FileUtils.listFiles(
                    maven.repository.resolve(installedFolder).toFile(),
                    FileFilterUtils.and(
                            REPOSITORY_FILES_FILTER,
                            FileFilterUtils.notFileFilter(FileFilterUtils.suffixFileFilter("pom"))
                    ),
                    FileFilterUtils.trueFileFilter()
            ).stream().map(File::toPath).collect(Collectors.toList());

            try (OutputStream output = Files.newOutputStream(archive)) {
                IOSupport.tar(files, output, aFile -> {
                    final Path filePath = aFile.toAbsolutePath();
                    return filePath.subpath(maven.repository.getNameCount(), filePath.getNameCount());
                });
            }

        }
    }

    @AllArgsConstructor
    public static class TargetFile extends Out {
        private final String src;
        private final Path dest;

        @SneakyThrows
        @Override
        public void save(Maven maven, Project.PomFile pom) {
            Path target = pom.target();
            final Path srcFile = target.resolve(src);
            Files.copy(srcFile, dest);
        }
    }
}
