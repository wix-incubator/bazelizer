package tools.jvm.mvn;

import com.google.common.io.Closer;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.cactoos.Output;
import org.cactoos.Proc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public interface Archive extends Proc<Output> {

    @SuppressWarnings({"DuplicatedCode", "UnstableApiUsage"})
    class Tar implements Archive {

        private final List<File> files;
        private final Path root;


        public Tar(Path repository) throws IOException {
            this(repository, FileFilterUtils.trueFileFilter());
        }

        public Tar(Path repository, IOFileFilter fileFilter) throws IOException {
            this(Files.walk(repository)
                    .map(Path::toFile)
                    .filter(f -> f.isFile() && fileFilter.accept(f))
                    .collect(Collectors.toList()), repository);
        }

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
}
