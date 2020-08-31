package tools.jvm.mvn;


import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.Closer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.cactoos.Output;
import org.cactoos.io.OutputTo;
import org.cactoos.io.TeeOutput;
import org.cactoos.io.WriterTo;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class Acts {


    /**
     * Install deps into M2_HOME
     */
    @Slf4j
    static class Deps implements Act {

        @Override
        public Project accept(Project project) {
            Path repo = project.m2Home().resolve("repository");
            project.deps().forEach(dep -> {
                final Path depFolder = dep.relativeTo(repo);
                //noinspection ResultOfMethodCallIgnored
                depFolder.toFile().mkdirs();
                String pref = dep.artifactId() + "-" + dep.version();
                Path jarFile = depFolder.resolve(pref + ".jar");
                copyTo(dep, jarFile);
                String pom = "<project>\n" +
                        "<modelVersion>4.0.0</modelVersion>\n" +
                        "<groupId>" + dep.groupId() + "</groupId>\n" +
                        "<artifactId>" + dep.artifactId() + "</artifactId>\n" +
                        "<version>" + dep.version() + "</version>\n" +
                        "</project>";
                Path pomFile = depFolder.resolve(pref + ".pom");
                log.info("Install: {}", dep);
                writeTo(pomFile, pom);
            });
            return project;
        }
    }


    /**
     * Run maven build.
     */
    static class MvnBuild implements Act {
        private final boolean offline;

        @SuppressWarnings("unused")
        public MvnBuild() {
            this(true);
        }

        public MvnBuild(boolean offline) {
            this.offline = offline;
        }

        @Override
        public Project accept(Project project) {
            final Args args = project.args();
            if (offline) args.append("--offline");
            args.append("clean");
            args.append("package");
            new BuildMvn().run(project);
            return project;
        }
    }

    /**
     * Output.
     */
    static class Outputs implements Act {
        @Override
        public Project accept(Project project) {
            final Path workDir = project.workDir();
            final Path target = workDir.resolve("target").toAbsolutePath();
            project.getOutputs().forEach(name -> {
                Path src = target.resolve(name.src());
                Path dest = Paths.get(name.dest());
                try {
                    Files.copy(src, dest);
                } catch (java.nio.file.NoSuchFileException e) {
                    throw new MvnException("no such file: " + src + ", within: [" + exists(target) + " ...]", e);
                } catch (IOException e) {
                    throw new MvnException(e);
                }

            });
            return project;
        }

        @SneakyThrows
        private String exists(Path target)  {
            return Files.walk(target, 2)
                    .limit(10).map(Path::toString).collect(Collectors.joining(", "));
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Slf4j
    static class PomMustache implements Act {

        @Override
        @lombok.SneakyThrows
        public Project accept(Project project) {
            final ByteSource pomFileTpl = project.pomXmlTpl();
            final CharSource tplSource = pomFileTpl.asCharSource(StandardCharsets.UTF_8);
            final File syntheticPom = newPomXML(project.workDir().toFile());
            MustacheFactory mf = new DefaultMustacheFactory();
            try (Reader tpl = tplSource.openStream()) {
                final StringWriter str = new StringWriter();
                final Output output = new TeeOutput(
                        new OutputTo(new WriterTo(syntheticPom)),
                        str
                );
                try (Writer dest = new PrintWriter(output.stream())) {
                    Mustache mustache = mf.compile(tpl, "template.mustache");
                    mustache.execute(dest, project);
                }

                log.info("{}", str);
            }
            project.args().append("-f", syntheticPom.getAbsolutePath());
            return project;
        }

        @lombok.SneakyThrows
        private static File newPomXML(File dir) {
            for (int i = 0; i < 1000; i++) {
                StringBuilder s = new StringBuilder();
                for (int j = 0; j < 10; j++) {
                    s.append((char) ThreadLocalRandom.current().nextInt('a', 'z'));
                }
                File file = new File(dir, "pom-" + s + "-" + i  + ".xml");
                if (!file.exists()) {
                    //noinspection UnstableApiUsage
                    com.google.common.io.Files.touch(file);
                    return file;
                }
            }
            throw new IllegalStateException("random file");
        }
    }


    /**
     * Prepare settings xml.
     */
    @Slf4j
    static class SettingsXml implements Act {

        @Override
        public Project accept(Project project) {
            final Path m2Home = project.m2Home();
            final Path settingsXml = m2Home.resolve("settings.xml").toAbsolutePath();
            final Path repository = m2Home.resolve("repository").toAbsolutePath();
            String xml =
                    "<settings xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.1.0 http://maven.apache.org/xsd/settings-1.1.0.xsd\" xmlns=\"http://maven.apache.org/SETTINGS/1.1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                            "<localRepository>" + repository + "</localRepository>\n" +
                            "</settings>";
            save(settingsXml, xml);
            log.info("{}", xml);
            project.args().append("-s", settingsXml.toString());
            return project;
        }

        @lombok.SneakyThrows
        private static void save(Path settingsXml, String xml) {
            Files.write(settingsXml, xml.getBytes(StandardCharsets.UTF_8));
        }
    }


    /**
     * Repository snapshot handler.
     */
    @SuppressWarnings("UnstableApiUsage")
    static class DefRepository implements Act {

        @Override
        @lombok.SneakyThrows
        public Project accept(Project project) {
            final Path tar = project.repoImage();
            final Path dest = project.m2Home().resolve("repository");
            final Closer closer = Closer.create();
            final TarArchiveInputStream ais = closer.register(new TarArchiveInputStream(
                    Files.newInputStream(tar, StandardOpenOption.READ)
            ));
            Iterator<TarArchiveEntry> iter = new AbstractIterator<TarArchiveEntry>() {
                @lombok.SneakyThrows
                @Override
                protected TarArchiveEntry computeNext() {
                    final TarArchiveEntry entry = ais.getNextTarEntry();
                    return entry != null ? entry : endOfData();
                }
            };
            try {
                iter.forEachRemaining(entry -> {
                    if (!ais.canReadEntryData(entry)) return;
                    File file = dest.resolve(entry.getName()).toFile();
                    if (entry.isDirectory()) {
                        defineDirectory(file);
                    } else {
                        defineDirectory(file.getParentFile());
                        copy(ais, file);
                    }
                });
            } finally {
                closer.close();
            }

            return project;
        }

        @lombok.SneakyThrows
        private static void copy(TarArchiveInputStream ais, File file) {
            Files.copy(ais, file.toPath());
        }

        @lombok.SneakyThrows
        private static void defineDirectory(File file) {
            if (!file.isDirectory() && !file.mkdirs())
                throw new IOException("failed to create directory " + file);
        }
    }




    /**
     * Create snapshot from the repository.
     */
    @Slf4j
    static class MkRepoSnapshot implements Act {

        @SuppressWarnings("UnstableApiUsage")
        @Override
        @lombok.SneakyThrows
        public Project accept(Project project) {
            final Path m2Home = project.m2Home();
            final Path src = m2Home.resolve("repository");
            final String dest = Iterables.getOnlyElement(project.getOutputs()).src();
            log.info("Archive: src={} dest={}", src, dest);
            final Closer closer = Closer.create();
            final TarArchiveOutputStream aos = closer.register(new TarArchiveOutputStream(
                    com.google.common.io.Files.asByteSink(new File(dest)).openBufferedStream()
            ));
            aos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            final Stream<Path> walk = Files.walk(src);
            closer.register(walk::close);
            try {
                walk.map(Path::toFile).filter(File::isFile).forEach(file -> writeEntry(src, aos, file));
                aos.finish();
            } finally {
                closer.close();
            }
            return project;
        }

        @lombok.SneakyThrows
        private void writeEntry(Path repository, TarArchiveOutputStream aos, File file) {
            final Path filePath = file.toPath();
            final ArchiveEntry entry = aos.createArchiveEntry(file,
                    repository.relativize(filePath).toString());
            aos.putArchiveEntry(entry);
            Files.copy(filePath, aos);
            aos.closeArchiveEntry();
        }
    }


    @AllArgsConstructor
    static class Mvn implements Act {
        private final boolean offline;

        @Override
        public Project accept(Project project) {
            final Args args = project.args();
            if (offline) args.append("--offline");
            args.append("clean");
            args.append("package");
            new BuildMvn().run(project);
            return project;
        }
    }


    static class Version implements Act {

        @Override
        public Project accept(Project project) {
            new BuildMvn().run(new Project.Wrap(project) {
                @Override
                public Args args() {
                    return new Args().append("--version");
                }
            });
            return project;
        }
    }



    @lombok.SneakyThrows
    private static void writeTo(Path pomFile, String pom) {
        java.nio.file.Files.write(pomFile, pom.getBytes(StandardCharsets.UTF_8));
    }

    @lombok.SneakyThrows
    private static void copyTo(Path src, Path jarFile) {
        Files.copy(src, jarFile);
    }

    @lombok.SneakyThrows
    private static void copyTo(Dep dep, Path jarFile) {
        copyTo(dep.source(), jarFile);
    }
}
