package tools.jvm.v2.mvn;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

@Slf4j
public class CmdRepository implements Runnable {

    @CommandLine.Option(names = {"--def"})
    public Path pomDeclarations;

    @CommandLine.Option(names = {"-rs", "--mk-snapshot"}, paramLabel = "PATH")
    public Path writeRepository;

    @Override
    public void run() {
        final Mvn maven = new Mvn();
        final PomFiles pomFiles = new PomFiles();
        new Manifest(pomDeclarations).items(PomFileDTO.class).forEach(dto -> {
            pomFiles.registerFile(dto.file);
        });

        final PomFiles.BuildsOrder builds = pomFiles.builds();
        log.info("Build order:\n{}", builds);

        builds.each(pomFile -> {
            final File location = pomFile.persisted(true);
        });
    }

    @Data
    private static class PomFileDTO {
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
