package tools.jvm.v2.mvn;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.cactoos.Input;
import org.cactoos.Output;
import org.cactoos.Scalar;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

@UtilityClass
@SuppressWarnings("ResultOfMethodCallIgnored")
public class TarUtils {

    @SneakyThrows
    public List<String> list(Path p) {
        try (TarArchiveInputStream is = new TarArchiveInputStream(Files.newInputStream(p, StandardOpenOption.READ))) {
            final ArrayList<String> nn = new ArrayList<>();
            for (TarArchiveEntry tarEntry; (tarEntry = is.getNextTarEntry()) != null;) {
                nn.add(tarEntry.getName());
            }
            return nn;
        }
    }

    @SuppressWarnings({"DuplicatedCode", "unused"})
    @SneakyThrows
    public void untar(Input tar, Path dest) {
        try(TarArchiveInputStream ais = new TarArchiveInputStream(tar.stream())) {
            final File destFile = dest.toFile();
            for (TarArchiveEntry tarEntry; (tarEntry = ais.getNextTarEntry()) != null;) {
                mkFile(ais, destFile, tarEntry);
            }
        }
    }

    @SuppressWarnings({"DuplicatedCode", "unused", "UnstableApiUsage"})
    @SneakyThrows
    public void tar(Collection<File> files, Output out, Function<File, Path> tarPath) {
        Scalar<TarArchiveOutputStream> aStream = () -> {
            final TarArchiveOutputStream aos = new TarArchiveOutputStream(out.stream());
            aos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            return aos;
        };
        try (TarArchiveOutputStream aos = aStream.value()) {
            for (File file : files) {
                final ArchiveEntry entry = aos.createArchiveEntry(file, tarPath.apply(file).toString());
                aos.putArchiveEntry(entry);
                com.google.common.io.Files.asByteSource(file).copyTo(aos);
                aos.closeArchiveEntry();
            }
            aos.finish();
        }
    }

    @SneakyThrows
    private static void mkFile(TarArchiveInputStream ais, File root, TarArchiveEntry tarEntry)  {
        File destPath = new File(root, tarEntry.getName());
        if (tarEntry.isDirectory()) {
            destPath.mkdirs();
        } else {
            if (!destPath.getParentFile().exists()) {
                destPath.getParentFile().mkdirs();
            }
            destPath.createNewFile();
            Files.copy(ais, destPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
