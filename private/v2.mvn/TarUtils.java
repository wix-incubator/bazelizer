package tools.jvm.v2.mvn;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.cactoos.Input;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
