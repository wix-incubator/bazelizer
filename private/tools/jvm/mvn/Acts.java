package tools.jvm.mvn;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.cactoos.Text;
import org.cactoos.func.UncheckedProc;
import org.cactoos.io.BytesOf;
import org.cactoos.io.InputOf;
import org.xembly.Xembler;

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

        /**
         * Go offline plugin.
         */
        public static final String GO_OFFLINE_PLUGIN =
                "de.qaware.maven:go-offline-maven-plugin:resolve-dependencies";

        private final Maven maven;

        private final List<String> profiles;

        MvnGoOffline(Maven maven, String...goals) {
            this.maven = maven;
            this.profiles = Lists.newArrayList(goals);
        }

        @Override
        public Project accept(Project project) {
            final Args args = new Args(project.args()).offline(false).append(GO_OFFLINE_PLUGIN);
            profiles.forEach(args::append);
            maven.run(project.toBuilder().args(args).build());
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
                    project.pomTemplate(),
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
    @Accessors(fluent = true)
    static class SettingsXml implements Act {
        public static final String ACTIVE_PROFILE = "bazelizer";

        /**
         * Ctor
         * @param rr repositories to activate
         */
        @SafeVarargs
        public SettingsXml(Iterable<Repositories.Repository>...rr) {
            for (Iterable<Repositories.Repository> r : rr) {
                Iterables.addAll(this.repositories, r);
            }
        }

        /**
         * Offline mode.
         */
        @SuppressWarnings("unused")
        @Setter
        private boolean offline = false;

        /**
         * Repositories.
         */
        @Getter
        private final List<Repositories.Repository> repositories = Lists.newArrayList();


        @SneakyThrows
        @Override
        @SuppressWarnings("UnstableApiUsage")
        public Project accept(Project project) {
            final Path m2Home = project.m2Home();
            final Path settingsXml = m2Home.resolve("settings.xml").toAbsolutePath();
            final Path repository = m2Home.resolve("repository").toAbsolutePath();
            Files.createDirectories(repository);

            final ImmutableMap.Builder<String, Object> builder = ImmutableMap.<String, Object>builder()
                    .put("localRepository", repository)
                    .put("offline", offline);

            if (!repositories.isEmpty()) {
                builder.put("activeProfile", ACTIVE_PROFILE);
                builder.put("profiles", ImmutableList.of(
                        ImmutableMap.of(
                                "profileId", ACTIVE_PROFILE,
                                "repositories", this.repositories))
                );
            }

            final Map<String, Object> props = builder.build();
            final Text xmlText = new Template.Mustache(
                    Resources.asByteSource(
                            Resources.getResource("settings.mustache.xml")
                    ),
                    props
            ).eval();

            Files.write(settingsXml, new BytesOf(xmlText).asBytes());
            log.info("\n{}", xmlText.asString());

            project.args().tag(Args.SettingsKey.SETTINGS_XML, settingsXml.toFile());

            return project;
        }
    }


    /**
     * Unarchive repository snapshot into M2_HOME.
     */
    @AllArgsConstructor
    @Slf4j
    static class Repository implements Act {

        @NonNull
        private final Path image;

        @Override
        @lombok.SneakyThrows
        public Project accept(Project project) {
            final Path repository = project.repository();
            Archive.extractTar(image, repository);
            project.args().tag(Args.SettingsKey.LOCAL_REPOSITORY, repository.toFile());
            if (log.isDebugEnabled()) {
                log.debug("Repository state: {}", repository);
                Files.find(repository, 30, (f,attr) -> !attr.isDirectory()).forEach(file ->
                        log.debug(" {}", repository.relativize(file) ));
            }

            return project;
        }
    }


    /**
     * Resolve relative path to optional parent project.
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    @Slf4j
    static class ParentPOM implements Act {

        @SneakyThrows
        @Override
        public Project accept(Project project) {
            final Path origParent = project.parentPom();
            final Path parentPomDir = SysProps.labelHex()
                    .map(hashedLabel -> {
                        final Path workDir = project.workDir();
                        final Path parentDir = workDir.resolve("_parent_" + hashedLabel);
                        final File file = parentDir.toFile();
                        file.mkdir();
                        file.deleteOnExit();
                        return parentDir;
                    }).orElseGet(() -> {
                        final Path workDir = project.workDir();
                        final Path parentDir = workDir.resolve(Texts.randomFileName("_parent_"));
                        final File file = parentDir.toFile();
                        file.mkdir();
                        file.deleteOnExit();
                        return parentDir;
                    });

            if (origParent != null) {
                final Path parentPomFile = parentPomDir.resolve("pom.xml");
                Files.copy(origParent, parentPomFile, StandardCopyOption.REPLACE_EXISTING);
                return project.toBuilder().parentPom(parentPomFile).build();
            }
            return project;
        }

    }

    /**
     * Install parent project.
     */
    @Slf4j
    @AllArgsConstructor
    static class InstallParentPOM implements Act {

        private final Maven maven;

        @Override
        public Project accept(Project project) {
            final Path parent = project.parentPom();
            if (parent != null) {
                final Path parentPomFile = parent.toAbsolutePath();
                Path parentDir = parentPomFile.getParent().normalize();
                log.info("Install parent pom file..");
                final Project parentProject = project.toBuilder()
                        .pom(parentPomFile)
                        .workDir(parentDir)
                        .args(new Args(project.args()).append("install"))
                        .build();

                maven.run(parentProject);
            }
            return project;
        }
    }

    /**
     * Write default outputs of maven build: jar file and installed data.
     */
    @AllArgsConstructor
    @Slf4j
    static class AppendDefaultOutputs implements Act {

        /**
         * Declared jar path
         */
        private final Path jar;

        /**
         * Declared tar archive.
         */
        private final Path artifact;

        @Override
        public Project accept(Project project) {
            final List<OutputFile> newOutputFiles = Lists.newArrayListWithCapacity(2);
            final Pom.Props bean = project.getPomProps();

            final IOFileFilter artifactFilter = FileFilterUtils.and(
                    new AbstractFileFilter() {
                        @Override
                        public boolean accept(File file) {
                            final Path path = file.toPath();
                            return path.startsWith(project.getArtifactFolder());
                        }
                    },
                    FileFilterUtils.notFileFilter(
                            FileFilterUtils.suffixFileFilter(".pom") // exclude pom from a pkg
                    )
            );

            final Collection<File> files = FileUtils.listFiles(
                    project.repository().toFile(),
                    artifactFilter,
                    FileFilterUtils.trueFileFilter()
            );

            final String pomFileJar = String.format("%s-%s.jar", bean.artifactId, bean.version);
            final Optional<File> installedJar = files.stream().filter(f -> f.getName().endsWith(pomFileJar)).findFirst();

            if (jar != null) {
                newOutputFiles.add(
                        installedJar.<OutputFile>map(f -> new OutputFile.Declared(f, jar.toAbsolutePath().toString()))
                        .orElseGet(() -> new OutputFile.Simple(pomFileJar, jar.toAbsolutePath().toString()))
                );
            }

            if (artifact != null) {

                final Archive archive = new Archive.TAR(
                        files,
                        aFile -> {
                            final Path filePath = aFile.toPath();
                            final Path filePathInRepo = project.repository().relativize(filePath);
                            log.debug("tar: {} as {}", aFile, filePathInRepo);
                            return filePathInRepo;
                        }
                );

                newOutputFiles.add(
                        new OutputFile.DeclaredProc(archive, artifact.toAbsolutePath().toString())
                );
            }

            final List<OutputFile> outputs = project.outputs();
            return project.toBuilder().outputs(
                    Lists.newArrayList(Iterables.concat(outputs, newOutputFiles))
            ).build();
        }
    }

    @AllArgsConstructor
    static class PomXembly implements Act {

        private final Iterable<XeSource> dirs;

        @SneakyThrows
        @Override
        public Project accept(Project project) {
            Pom pom = new Pom.StringOf(project.pom());

            XML xml = pom.xml();
            for (XeSource dir : dirs) {
                xml = new XMLDocument(
                        new Xembler(new DirectivesNs(dir.value())).apply(xml.node())
                );
            }

            return null;
        }
    }

    /**
     * Archive artifact binaries as is without pom
     */
    @Slf4j
    @AllArgsConstructor
    @Deprecated
    static class ArtifactPredefOutputs implements Act {

        public static final String FLAG_DEF_JAR = "@DEF_JAR";

        public static final String FLAG_PKG = "@DEF_PKG";

        private final Map<String,String> settings;

        @SneakyThrows
        @Override
        public Project accept(Project project) {
            final ArrayList<OutputFile> outputs = Lists.newArrayList(project.outputs());
            final Pom.Props bean = new Pom.StringOf(
                    project.pom()
            ).props();

            Map<String,String> settings = this.settings != null ? this.settings : Collections.emptyMap();

            final Path repository = project.repository();
            final Path thisArtifactFolder = new Dep.Simple(null,
                    bean.getGroupId(),
                    bean.getArtifactId(),
                    bean.getVersion()
            ).artifactFolder(repository);
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