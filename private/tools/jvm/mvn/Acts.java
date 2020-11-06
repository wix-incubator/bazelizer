package tools.jvm.mvn;


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
import org.cactoos.Input;
import org.cactoos.Output;
import org.cactoos.Text;
import org.cactoos.func.UncheckedProc;
import org.cactoos.io.BytesOf;
import org.cactoos.io.InputOf;
import org.cactoos.io.InputStreamOf;
import org.cactoos.io.OutputTo;
import org.xembly.Directives;
import org.xembly.Xembler;

import java.io.File;
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
    static class PomFile implements Act {

        @Override
        @lombok.SneakyThrows
        public Project accept(Project project) {
            final Path syntheticPom = project.pom();

            final Pom pom = new Pom.Cached(
                    new Pom.PomXembly(
                            new Pom.Standard(project.pomTemplate()),
                            project
                    )
            );

            final XML pomXml = pom.xml();

            Files.copy(new InputStreamOf(pomXml.toString()), syntheticPom);

            if (log.isDebugEnabled()) {
                log.debug("\n{}", pomXml);
            }

            return project.toBuilder().pomXML(pom).build();
        }
    }


    @AllArgsConstructor
    static class GlobalSettingsXml implements Act {

        public GlobalSettingsXml(Path settings, Path outputManifest) {
            this(new InputOf(settings), new OutputTo(outputManifest));
        }

        private final Input globalSettingsXmlPath;

        private final Output manifestXml;

        @SneakyThrows
        @Override
        public Project accept(Project project) {

            final XML settingsXml = new XMLDocument(
                    new InputStreamOf(globalSettingsXmlPath)
            );

            final String curLocalRepository = settingsXml.xpath("/settings/localRepository/text()").get(0);
            final ImmutableMap<String, String> props = ImmutableMap.of(
                    "id", "global_cache",
                    "name", "Bazel's global m2 cache",
                    "url", new File(curLocalRepository).toURI().toString()
            );
            final String bazelM2Cache = "bzl_m2_cache";
            final Directives dirs = new Directives()
                    .xpath("/settings/localRepository")
                    .set("{{ localRepository }}")
                    // profile to refer to global cache repo
                    .xpath("/settings")
                        .addIf("profiles")
                        .add("profile")
                        .add(ImmutableMap.of("id", bazelM2Cache))
                            .add("repositories")
                                .add("repository")
                                .add(props)
                                .up()
                            .up()
                            .add("pluginRepositories")
                                .add("pluginRepository")
                                .add(props)
                    // activate profile
                    .xpath("/settings")
                        .addIf("activeProfiles")
                        .add(ImmutableMap.of("activeProfile", bazelM2Cache));

            final XMLDocument buildSettingsXmlTpl = new XMLDocument(
                    new Xembler(dirs).apply(settingsXml.node())
            );

            final RunManifest runManifest = new RunManifest.Builder()
                    .settingsXmlTemplate(buildSettingsXmlTpl.toString())
                    .build();

            project.outputs().add(
                    new OutputFile.Content(
                            new InputOf(runManifest.asString()),
                            manifestXml
                    )
            );

            return project;
        }
    }

    /**
     * Prepare settings xml.
     */
    @Slf4j
    @Accessors(fluent = true)
    static class SettingsXml implements Act {

        private final RunManifest manifest;

        /**
         * Offline mode.
         */
        @SuppressWarnings("unused")
        @Setter
        private boolean offline = false;


        public SettingsXml(RunManifest manifest) {
            this.manifest = manifest;
        }


        @SneakyThrows
        @Override
        public Project accept(Project project) {
            final Path m2Home = project.m2Directory();
            final Path settingsXml = m2Home.resolve("settings.xml").toAbsolutePath();
            final Path repository = m2Home.resolve("repository").toAbsolutePath();
            Files.createDirectories(repository);

            final ImmutableMap<String, Object> props = ImmutableMap.<String, Object>builder()
                    .put("localRepository", repository)
                    .put("offline", offline)
                    .build();

            final Text xml = new Template.Mustache(
                    new InputOf(this.manifest.getSettingsXml()),
                    props
            ).eval();

            Files.write(settingsXml, new BytesOf(xml).asBytes());
            log.info("\n{}", xml.asString());

            project.args().tag(Args.FlagsKey.SETTINGS_XML, settingsXml.toFile());
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
            project.args().tag(Args.FlagsKey.LOCAL_REPOSITORY, repository.toFile());
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
            final Pom pom = project.getPomXML();

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

            final String pomFileJar = String.format("%s-%s.jar", pom.artifactId(), pom.version());
            final Optional<File> installedJar = files.stream().filter(f -> f.getName().endsWith(pomFileJar)).findFirst();

            if (jar != null) {
                newOutputFiles.add(
                        installedJar.<OutputFile>map(
                                contentFile -> new OutputFile.Content(
                                        new InputOf(contentFile),
                                        new OutputTo(jar)
                                )
                        ).orElseGet(() -> new OutputFile.TargetFolderFile(
                                pomFileJar,
                                jar.toAbsolutePath().toString())
                        )
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
                        new OutputFile.ArchiveOf(archive, new OutputTo(artifact.toAbsolutePath()))
                );
            }

            final List<OutputFile> outputs = project.outputs();
            return project.toBuilder().outputs(
                    Lists.newArrayList(Iterables.concat(outputs, newOutputFiles))
            ).build();
        }
    }
}