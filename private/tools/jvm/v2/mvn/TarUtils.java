package tools.jvm.v2.mvn;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import com.google.common.io.Closer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.cactoos.Input;
import org.cactoos.Output;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

@UtilityClass
@SuppressWarnings("ResultOfMethodCallIgnored")
public class TarUtils {

    @SneakyThrows
    public List<String> list(Path p) {
        try (TarArchiveInputStream is = open(p)) {
            final ArrayList<String> nn = Lists.newArrayList();
            new Iter(is).forEachRemaining(x -> nn.add(x.getName()));
            return nn;
        }
    }

    @SuppressWarnings({"DuplicatedCode", "unused"})
    @SneakyThrows
    public void untar(Input tar, Path dest) {
        try(TarArchiveInputStream ais = new TarArchiveInputStream(tar.stream())) {
            final Iterator<TarArchiveEntry> iter = new Iter(ais);
            final File destFile = dest.toFile();
            iter.forEachRemaining(tarEntry -> {
                mkFile(ais, destFile, tarEntry);
            });
        }
    }

    @SuppressWarnings({"DuplicatedCode", "unused", "UnstableApiUsage"})
    @SneakyThrows
    public void tar(Collection<File> files, Output out, Function<File, Path> tarPath) {
        final Closer closer = Closer.create();
        final TarArchiveOutputStream aos = closer.register(new TarArchiveOutputStream(out.stream()));
        aos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        try {
            for (File file : files) {
                final ArchiveEntry entry = aos.createArchiveEntry(file, tarPath.apply(file).toString());
                aos.putArchiveEntry(entry);
                com.google.common.io.Files.asByteSource(file).copyTo(aos);
                aos.closeArchiveEntry();
            }
            aos.finish();
        } finally {
            closer.close();
        }
    }

    public static TarArchiveInputStream open(Path p) throws IOException {
        return new TarArchiveInputStream(Files.newInputStream(p, StandardOpenOption.READ));
    }

    @AllArgsConstructor
    private static class Iter extends AbstractIterator<TarArchiveEntry> {
        final TarArchiveInputStream ais;
        @SneakyThrows
        @Override
        protected TarArchiveEntry computeNext() {
            final TarArchiveEntry entry = ais.getNextTarEntry();
            return entry != null ? entry : endOfData();
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
