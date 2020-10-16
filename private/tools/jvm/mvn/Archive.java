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
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
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
                    file -> {
                        System.out.println(file);
                        return dir.relativize(file.toPath());
                    }
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

    @SuppressWarnings("UnstableApiUsage")
    @SneakyThrows
    static void extractTar(Path tar, Project project) {
        final Path dest = project.m2Home().resolve("repository");
        final Closer closer = Closer.create();
        Consumer<File> defineDirectory = file -> {
            if (!file.isDirectory() && !file.mkdirs())
                throw new IllegalStateException("failed to create directory " + file);
        };
        final TarArchiveInputStream ais = closer.register(new TarArchiveInputStream(
                Files.newInputStream(tar, StandardOpenOption.READ)
        ));
        Iterable<TarArchiveEntry> iter = new TarList(ais, false);
        try {
            iter.forEach(entry -> {
                if (!ais.canReadEntryData(entry)) return;
                File file = dest.resolve(entry.getName()).toFile();
                if (entry.isDirectory()) {
                    defineDirectory.accept(file);
                } else {
                    defineDirectory.accept((file.getParentFile()));
                    try {
                        Files.copy(ais, file.toPath());
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }

                }
            });
        } finally {
            closer.close();
        }
    }

    @AllArgsConstructor
    @Deprecated
    class TarList implements Iterable<TarArchiveEntry> {
        private final TarArchiveInputStream ais;
        private final boolean close;

        public TarList(Path file) throws IOException {
            this(new TarArchiveInputStream(Files.newInputStream(file, StandardOpenOption.READ)), true);
        }

        @Override
        public Iterator<TarArchiveEntry> iterator() {
            Iterator<TarArchiveEntry> iter = new AbstractIterator<TarArchiveEntry>() {
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

            return iter;
        }
    }
}
