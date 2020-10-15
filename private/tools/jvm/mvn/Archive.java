package tools.jvm.mvn;

import com.google.common.collect.AbstractIterator;
import com.google.common.io.Closer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.cactoos.BiProc;
import org.cactoos.Output;
import org.cactoos.Proc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public interface Archive extends Proc<Output> {


    /**
     *
     */
    @SuppressWarnings({"DuplicatedCode", "UnstableApiUsage"})
    class Tar implements Archive {

        private final List<File> files;
        private final Path root;


        /**
         * Ctor.
         * @param repository repo
         * @throws IOException if any
         */
        public Tar(Path repository) throws IOException {
            this(repository, FileFilterUtils.trueFileFilter());
        }

        /**
         * Ctro.
         * @param repository
         * @param fileFilter
         * @throws IOException
         */
        public Tar(Path repository, IOFileFilter fileFilter) throws IOException {
            this(Files.walk(repository)
                    .map(Path::toFile)
                    .filter(f -> f.isFile() && fileFilter.accept(f))
                    .collect(Collectors.toList()), repository);
        }

        /**
         * Ctro.
         * @param files
         * @param root
         */
        public Tar(List<File> files, Path root) {
            this.files = files;
            this.root = root;
        }


        @Override
        public void exec(Output out) throws Exception {
            final Closer closer = Closer.create();
            final TarArchiveOutputStream aos = closer.register(new TarArchiveOutputStream(out.stream()));
            aos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            try {
                for (File file : files) {
                    final ArchiveEntry entry = aos.createArchiveEntry(file,
                            root.relativize(file.toPath()).toString());
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
