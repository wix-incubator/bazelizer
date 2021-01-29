package tools.jvm.v2.mvn;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Closer;
import lombok.SneakyThrows;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.cactoos.Output;
import org.cactoos.io.InputOf;
import org.cactoos.io.OutputTo;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

@CommandLine.Command(name = "run")
public class CmdBuild implements Runnable {

    @CommandLine.Option(names = {"--pom"}, required = true,
            paramLabel = "POM", description = "the pom xml template file")
    public Path pomFile;

    @CommandLine.Option(names = {"--m2-repository"},
            paramLabel = "REPO", description = "the repository tar")
    public Path repositoryTar;

    @CommandLine.Option(names = {"--deps"}, paramLabel = "DEPS", description = "the deps manifest")
    public Path deps;

    @CommandLine.Option(names = {"-O"}, description = "declared bazel output -> relatice file path /target")
    public Map<String, String> outputs = ImmutableMap.of();

    @CommandLine.Option(names = {"-wid", "--write-artifact"}, paramLabel = "P",
            description = "write archived artifact from repo, except default jar")
    public Path writeInstalledArtifact;

    @CommandLine.Option(names = {"-wdj", "--write-jar"}, paramLabel = "P",
            description = "write default jar")
    public Path writeDefaultJar;

    @SneakyThrows
    @Override
    public void run() {
        final Iterable<Dep> deps = Dep.load(
                new Manifest(this.deps)
        );

        final Mvn builds = new Mvn(
                new InputOf(repositoryTar)
        );

        builds.installDeps(deps);

        final Pom pom = new Pom.Std(new InputOf(pomFile));
//                .update(
//                new PomUpdate.PomStruc(),
//                new PomUpdate.PomDropDeps(),
//                new PomUpdate.AppendDeps(deps)
//                );

        builds.exec(saveFile(pom), Arrays.asList("clean", "install"), Collections.emptyList());

        final Path target = pomFile.getParent().resolve("target");
        writeJar(target, pom);
        writeCustom(target);
        writeArchivedFolder(builds, pom);
    }


    @SneakyThrows
    private File saveFile(Pom pom) {
        final Path abs = pomFile.getParent().resolve("pom." + Mvn.LABEL + ".xml");
        if (Files.notExists(abs)) {
            Files.write(abs, pom.toString().getBytes());
        }
        return abs.toFile();
    }

    private void writeCustom(Path target) {
        outputs.forEach((relSrc, destAbs) -> {
            try {
                Files.copy(target.resolve(relSrc), Paths.get(destAbs));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @SneakyThrows
    private void writeJar(Path target, Pom pom) {
        Files.copy(
                target.resolve(String.format("%s-%s.jar", pom.artifactId(), pom.version())),
                writeDefaultJar
        );
    }

    @SneakyThrows
    private void writeArchivedFolder(Mvn mvn, Pom pom) {
        final Output output = new OutputTo(writeInstalledArtifact);
        final Path installedFolder = pom.folder();
        final IOFileFilter artifactFilter = FileFilterUtils.and(
                new AbstractFileFilter() {
                    @Override
                    public boolean accept(File file) {
                        final Path path = file.toPath();
                        return path.startsWith(installedFolder);
                    }
                },
                FileFilterUtils.notFileFilter(FileFilterUtils.suffixFileFilter(".pom")) // exclude pom from a pkg
        );

        final Collection<File> files = FileUtils.listFiles(
                mvn.repository().toFile(), artifactFilter, FileFilterUtils.trueFileFilter()
        );
        final Function<File, Path> tarPath = aFile -> {
            final Path filePath = aFile.toPath();
            //log.debug("tar: {} as {}", aFile, filePathInRepo);
            return mvn.repository().relativize(filePath);
        };
        try(TarArchiveOutputStream aos = new TarArchiveOutputStream(output.stream())) {
            aos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            for (File file : files) {
                final ArchiveEntry entry = aos.createArchiveEntry(file, tarPath.apply(file).toString());
                aos.putArchiveEntry(entry);
                try (InputStream is = Files.newInputStream(file.toPath())) {
                    IOUtils.copy(is, aos);
                }
                aos.closeArchiveEntry();
            }
            aos.finish();
        }
    }


}
