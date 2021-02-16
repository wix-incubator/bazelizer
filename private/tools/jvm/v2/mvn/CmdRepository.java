package tools.jvm.v2.mvn;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.cactoos.io.OutputTo;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static tools.jvm.v2.mvn.Mvn.REPOSITORY_FILES_FILTER;

@CommandLine.Command(name = "build-repository")
@Slf4j
public class CmdRepository implements Runnable {


    /**
     * Go native offline plugin.
     */
    public static final String GO_OFFLINE_NATIVE_PLUGIN_TASK =
            "dependency:go-offline";

    /**
     * Go offline plugin.
     */
    public static final String GO_OFFLINE_PLUGIN_TASK =
            "de.qaware.maven:go-offline-maven-plugin:resolve-dependencies";

    /**
     * Goal.
     */
    public static final String INSTALL =
            "install";

    @CommandLine.Option(names = {"--settings"}, paramLabel = "PATH")
    public Path settingsXmlToUse;

    @CommandLine.Option(names = {"--def"}, paramLabel = "PATH")
    public Path pomDeclarations;

    @CommandLine.Option(names = {"--mk-snapshot"}, paramLabel = "PATH")
    public Path writeRepositoryDest;

    @SneakyThrows
    @Override
    public void run() {
        final Mvn maven = new Mvn(
                new SettingsXml.Json(settingsXmlToUse)
        );
        final Builds pomFiles = new Builds();
        new Manifest(pomDeclarations).items(PomFileInfo.class).forEach(dto -> {
            pomFiles.registerFile(dto.file);
        });

        final Builds.BuildsOrder builds = pomFiles.travers();
        log.info("Build order:\n{}", builds);

        builds.each(pomFile -> {
            final File location = pomFile.persisted();
            maven.exec(
                    location,
                    Arrays.asList(
                            GO_OFFLINE_NATIVE_PLUGIN_TASK,
                            GO_OFFLINE_PLUGIN_TASK,
                            INSTALL
                    ),
                    Collections.emptyList()
            );
        });

        writeSnapshot(maven);
    }

    public void writeSnapshot(Mvn maven) throws IOException {
        final Path repository = maven.repository();
        final Collection<File> files = FileUtils.listFiles(
                repository.toFile(),
                REPOSITORY_FILES_FILTER,
                FileFilterUtils.directoryFileFilter() // recursive
        );
        TarUtils.tar(files, new OutputTo(writeRepositoryDest), file -> repository.relativize(file.toPath()));
        log.info("Build repository snapshot: {}", FileUtils.byteCountToDisplaySize(writeRepositoryDest.toFile().length()));
    }

    @Data
    private static class PomFileInfo {
        @SerializedName("file")
        private Path file;
        @SerializedName("flags_line")
        private List<String> flags;

//        public Args args() {
//            return Optional.ofNullable(flags)
//                    .filter(d -> !d.isEmpty())
//                    .map(flags -> {
//                        final String line = String.join(" ", flags);
//                        return new Args().parseCommandLine(line);
//                    }).orElse(new Args());
//        }
    }
}
