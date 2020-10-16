package tools.jvm.mvn;

import com.google.common.collect.AbstractIterator;
import com.google.common.io.Closer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.cactoos.Output;
import org.cactoos.Proc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface Archive extends Proc<Output> {



    /**
     * Tar.
     */
    @SuppressWarnings({"DuplicatedCode", "UnstableApiUsage"})
    @AllArgsConstructor
    class TAR implements Archive {

        private final Collection<File> files;
        private final Function<File, Path> tarPath;


        @Override
        public void exec(Output out) throws Exception {
            final Closer closer = Closer.create();
            final TarArchiveOutputStream aos = closer.register(new TarArchiveOutputStream(out.stream()));
            aos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            try {
                for (File file : files) {
                    final ArchiveEntry entry = aos.createArchiveEntry(file, tarPath.apply(file).toString());
                    aos.putArchiveEntry(entry);
                    Files.copy(file.toPath(), aos);
                    aos.closeArchiveEntry();
                }
                aos.finish();
            } finally {
                closer.close();
            }
        }
    }

    /**
     * Tar all directory.
     */
    @SuppressWarnings({"DuplicatedCode"})
    @ToString
    class TarDirectory implements Archive {

        private final Archive archive;

        public TarDirectory(Path dir) throws IOException {
            archive = new Archive.TAR(
                    Archive.listFiles(dir),
                    file -> dir.relativize(file.toPath())
            );
        }

        @Override
        public void exec(Output out) throws Exception {
            archive.exec(out);
        }
    }


    /**
     * List files in dir and make it relative
     * @param repository repo
     * @return relative paths
     */
    static Collection<File> listFiles(Path repository) throws IOException {
        return Files.find(repository, Integer.MAX_VALUE, (x,y) -> y.isRegularFile())
                .map(Path::toFile)
                .collect(Collectors.toList());
    }

    @SuppressWarnings({"UnstableApiUsage"})
    @SneakyThrows
    static void extractTar(Path tar, Path dest) {
//        final Path dest = project.m2Home().resolve("repository");
//        System.out.println("Extracting " + tar + " to " + dest);
        final Closer closer = Closer.create();

        final TarArchiveInputStream ais = closer.register(new TarArchiveInputStream(
                Files.newInputStream(tar, StandardOpenOption.READ)
        ));
        Iterable<TarArchiveEntry> iter = new LSTar(ais, false);
        try {
            final File destFile = dest.toFile();
            iter.forEach(tarEntry -> {
                mkFile(ais, destFile, tarEntry);
            });
        } finally {
            closer.close();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SneakyThrows
    static void mkFile(TarArchiveInputStream ais, File root, TarArchiveEntry tarEntry)  {
        File destPath = new File(root, tarEntry.getName());
//        System.out.println("Write " + destPath + " by " + tarEntry.getName());
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

    @SuppressWarnings("NullableProblems")
    @AllArgsConstructor
    class LSTar implements Iterable<TarArchiveEntry> {
        private final TarArchiveInputStream ais;
        private final boolean close;

        public LSTar(Path file) throws IOException {
            this(new TarArchiveInputStream(Files.newInputStream(file, StandardOpenOption.READ)), true);
        }

        @Override
        public Iterator<TarArchiveEntry> iterator() {
            return new AbstractIterator<TarArchiveEntry>() {
                @SneakyThrows
                @Override
                protected TarArchiveEntry computeNext() {
                    final TarArchiveEntry entry = ais.getNextTarEntry();
                    return entry != null ? entry : fin();
                }

                private TarArchiveEntry fin() {
                    if (close) {
                        IOUtils.closeQuietly(ais);
                    }
                    return endOfData();
                }
            };
        }
    }
}
