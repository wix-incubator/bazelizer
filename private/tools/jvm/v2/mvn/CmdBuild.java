package tools.jvm.v2.mvn;

import com.google.common.collect.ImmutableMap;
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

import static org.apache.commons.io.filefilter.FileFilterUtils.*;


@CommandLine.Command(name = "run")
public class CmdBuild implements Runnable {

    @CommandLine.Option(names = {"--pom"}, required = true,
            paramLabel = "POM", description = "The pom xml file")
    public Path pomFile;

    @CommandLine.Option(names = {"--m2-repository"},
            paramLabel = "REPO", description = "The repository tar")
    public Path repositoryTar;

    @CommandLine.Option(names = {"--deps"}, paramLabel = "DEPS",
            description = "The deps manifest")
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
                new InputOf(repositoryTar),
                new SettingsXml.Default()
        );

        builds.installDeps(deps);

        final PomFile aPomFile = new PomFile.Simple(pomFile.toFile()).update(
                new PomUpdate.PomStruc(),
                new PomUpdate.PomDropDeps(),
                new PomUpdate.AppendDeps(deps)
        );

        builds.execOffline(
                aPomFile.persisted(),
                Arrays.asList("clean", "install"),
                Collections.emptyList()
        );

        final Path target = pomFile.toAbsolutePath().getParent().resolve("target");
        writeJar(target, aPomFile.pom());
        writeCustom(target);
        writeArchivedFolder(builds, aPomFile.pom());
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
        Files.copy(target.resolve(String.format("%s-%s.jar", pom.artifactId(), pom.version())), writeDefaultJar);
    }

    @SneakyThrows
    private void writeArchivedFolder(Mvn mvn, Pom pom) {
        final Output output = new OutputTo(writeInstalledArtifact);
        final Path installedFolder = pom.folder();
        final Path repository = mvn.repository();
        final Collection<File> files = FileUtils.listFiles(
                repository.resolve(installedFolder).toFile(),
                and(Mvn.REPOSITORY_FILES_FILTER, notFileFilter(suffixFileFilter("pom"))),
                trueFileFilter()
        );

        TarUtils.tar(files, output, aFile -> {
            final Path filePath = aFile.toPath().toAbsolutePath();
            return filePath.subpath(repository.getNameCount(), filePath.getNameCount());
        });
    }


}
