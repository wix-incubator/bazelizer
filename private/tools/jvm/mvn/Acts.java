package tools.jvm.mvn;


import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.cactoos.Text;
import org.cactoos.func.UncheckedProc;
import org.cactoos.io.InputOf;
import org.cactoos.io.OutputTo;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

@UtilityClass
public final class Acts {


    /**
     * Install deps into M2_HOME folder.
     * Generate simple synthetic pom file inside.
     */
    @Slf4j
    static class Deps implements Act {

        @Override
        @SneakyThrows
        public Project accept(Project project) {
            Path repo = project.repository();

            project.deps().forEach(dep -> {
                new UncheckedProc<>(dep.installTo()).exec(repo);
                log.info("install: {}", dep);
            });

            log.info("Installed deps {} into {}", Iterables.size(project.deps()), project.repository());
            return project;
        }
    }


    /**
     * Maven build. Turn off online resolving dependencies.
     */
    @AllArgsConstructor
    static class MvnBuild implements Act {
        private final Maven maven;

        @Override
        public Project accept(Project project) {
            project.args()
                    .offline(true)
                    .append("clean", "install");

            maven.run(project);
            return project;
        }
    }

    /**
     * Run maven build. Allow to fetch dependencies from the web.
     */
    @Slf4j
    @Accessors(chain = true, fluent = true)
    static class MvnGoOffline implements Act {

        private final Maven maven;

        // should add it because of https://github.com/qaware/go-offline-maven-plugin#usage
        @Setter
        private boolean compile = false;

        @Setter
        private boolean install = false;


        MvnGoOffline(Maven maven) {
            this.maven = maven;
        }

        @Override
        public Project accept(Project project) {
            log.info("Eagerly fetch dependencies to go offline...");

            final Args args = new Args().offline(false)
                    .append("de.qaware.maven:go-offline-maven-plugin:resolve-dependencies");

            if (compile) args.append("compile");
            if (install) args.append("install");

            maven.run(
                    project.toBuilder().args(args).build()
            );
            return project;
        }
    }

    /**
     * Write registered outputs.
     */
    static class Outputs implements Act {

        @SneakyThrows
        @Override
        public Project accept(Project project) {
            for (OutputFile output : project.outputs()) {
                output.exec(project);
            }
            return project;
        }

    }

    /**
     * Process pom file from template.
     */
    @Slf4j
    static class POM implements Act {

        @Override
        @lombok.SneakyThrows
        public Project accept(Project project) {
            final Project.ProjectView props = project.toView();
            final Path syntheticPom = project.pom();
            final Text renderedTpl = new Template.Mustache(
                    project.pomTpl(),
                    props
            ).eval();

            Files.copy(new InputOf(renderedTpl, StandardCharsets.UTF_8).stream(),syntheticPom);

            if (log.isDebugEnabled()) {
                log.debug("\n{}", renderedTpl.asString()); }
            return project;
        }
    }


    /**
     * Prepare settings xml.
     */
    @Slf4j
    static class SettingsXml implements Act {

        @SneakyThrows
        @Override
        public Project accept(Project project) {
            final Path m2Home = project.m2Home();
            final Path settingsXml = m2Home.resolve("settings.xml").toAbsolutePath();
            final Path repository = m2Home.resolve("repository").toAbsolutePath();
            Files.createDirectories(repository);
            String xml = "<settings xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.1.0 http://maven.apache.org/xsd/settings-1.1.0.xsd\" " +
                    "xmlns=\"http://maven.apache.org/SETTINGS/1.1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                            "   <localRepository>" + repository + "</localRepository>\n" +
                            "</settings>";

            save(settingsXml, xml);
            log.debug("\n{}", xml);
            return project;
        }

        @lombok.SneakyThrows
        private static void save(Path settingsXml, String xml) {
            Files.write(settingsXml, xml.getBytes(StandardCharsets.UTF_8));
        }
    }


    /**
     * Unarchive repository snapshot into M2_HOME.
     */
    @AllArgsConstructor
    @Slf4j
    static class Repository implements Act {

        private final Path image;

        @Override
        @lombok.SneakyThrows
        public Project accept(Project project) {
            final Path repository = project.repository();
            if (image != null)
                Archive.extractTar(image, repository);
            if (log.isDebugEnabled()) {
                log.debug("Repository state:");
                Files.find(repository, 30, (f,attr) -> !attr.isDirectory()).forEach(file -> {
                    log.debug(" {}", repository.relativize(file) );
                });
            }

            return project;
        }
    }

    /**
     * Create snapshot from the repository.
     */
    @Slf4j
    @Deprecated
    static class RepositoryArchiver implements Act {

        @Override
        @lombok.SneakyThrows
        public Project accept(Project project) {
            final Path m2Home = project.m2Home();
            final Path src = m2Home.resolve("repository");
            final String dest = Iterables.getOnlyElement(project.outputs()).src();
            log.debug("Archive: src={} dest={}", src, dest);
            final File destFile = new File(dest);
            new Archive.TarDirectory(src).exec(
                    new OutputTo(destFile)
            );
            log.info("Repository archive created: {}", FileUtils.byteCountToDisplaySize(destFile.length()));
            return project;
        }
    }

    /**
     * Resolve relative path to optional parent project.
     */
    @SuppressWarnings({"UnstableApiUsage", "ResultOfMethodCallIgnored"})
    @Slf4j
    static class ParentPOM implements Act {

        @SneakyThrows
        @Override
        public Project accept(Project project) {
            final Path origParent = project.pomParent();
            final Path parentPomDir = Optional.ofNullable(System.getProperty("tools.jvm.mvn.BazelLabelName"))
                    .map(name -> {
                        final String hashedLabel = Hashing.murmur3_32().hashString(name,
                                StandardCharsets.UTF_8).toString();
                        final Path workDir = project.workDir();
                        final Path parentDir = workDir.resolve("_parent_" + hashedLabel);
                        final File file = parentDir.toFile();
                        file.mkdir();
                        return parentDir;
                    }).orElseGet(() -> {
                        final Path workDir = project.workDir();
                        final Path parentDir = workDir.resolve(RandomText.randomFileName("parent"));
                        final File file = parentDir.toFile();
                        file.mkdir();
                        file.deleteOnExit();
                        return parentDir;
                    });
            if (origParent != null) {
                final Path parentPomFile = parentPomDir.resolve("pom.xml");
                Files.copy(origParent, parentPomFile, StandardCopyOption.REPLACE_EXISTING);
                return project.toBuilder().pomParent(parentPomFile).build();
            }
            return project;
        }

    }

    /**
     * Install parent project.
     */
    @Slf4j
    static class InstallParentPOM implements Act {

        @Override
        public Project accept(Project project) {
            final Path origParent = project.pomParent();
            if (origParent != null) {
                final Path parentPomFile = origParent.toAbsolutePath();
                Path parentDir = parentPomFile.getParent().normalize();
                log.info("Install parent project into repository...");
                final Project parentProject = project.toBuilder()
                        .pom(parentPomFile)
                        .workDir(parentDir)
                        .args(new Args().append("install"))
                        .build();

                new Maven.BazelInvoker().run(parentProject);
            }
            return project;
        }
    }


    /**
     * Archive artifact binaries as is without pom
     */
    @Slf4j
    @AllArgsConstructor
    static class ArtifactPredefOutputs implements Act {

        public static final String FLAG_DEF_JAR = "@DEF_JAR";

        public static final String FLAG_PKG = "@DEF_PKG";

        public static final String FLAG_MVN_COORDS = "@MVN_COORDS";

        private final Map<String,String> settings;

        @SneakyThrows
        @Override
        public Project accept(Project project) {
            final ArrayList<OutputFile> outputs = Lists.newArrayList(project.outputs());
            final Pom.Props bean = new Pom.XPath(
                    new InputOf(project.pom())
            ).value();

            Map<String,String> settings = this.settings != null ? this.settings : Collections.emptyMap();

            final Path repository = project.repository();
            final Path thisArtifactFolder = new Dep.Simple(null,
                    bean.getGroupId(),
                    bean.getArtifactId(),
                    bean.getVersion()
            ).relativeTo(repository);
            final IOFileFilter artifactFilter = FileFilterUtils.and(
                    new AbstractFileFilter() {
                        @Override
                        public boolean accept(File file) {
                            final Path path = file.toPath();
                            return path.startsWith(thisArtifactFolder);
                        }
                    },
                    FileFilterUtils.notFileFilter(
                            FileFilterUtils.suffixFileFilter(".pom") // exclude pom from a pkg
                    )
            );

            final Collection<File> files = FileUtils.listFiles(
                    repository.toFile(), artifactFilter, FileFilterUtils.trueFileFilter()
            );

            Optional.ofNullable(settings.get(FLAG_PKG)).ifPresent(dest -> {
                final Archive archive = new Archive.TAR(
                        files,
                        aFile -> {
                            final Path filePath = aFile.toPath();
                            final Path filePathInRepo = repository.relativize(filePath);
                            log.debug("tar: {} as {}", aFile, filePathInRepo);
                            return filePathInRepo;
                        }
                );

                outputs.add(
                        new OutputFile.DeclaredProc(archive, dest)
                );
            });

            Optional.ofNullable(settings.get(FLAG_DEF_JAR)).ifPresent(dest -> {
                final String pomFileJar = String.format("%s-%s.jar", bean.artifactId, bean.version);
                final Optional<File> installed = files.stream().filter(f -> f.getName().endsWith(pomFileJar)).findFirst();

                outputs.add(
                        installed.<OutputFile>map(f -> new OutputFile.Declared(f, dest))
                                .orElseGet(() -> new OutputFile.Simple(pomFileJar, dest))
                );
            });

            return project.toBuilder().outputs(outputs).build();
        }
    }
}