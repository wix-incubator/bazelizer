package tools.jvm.v2.mvn;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class TarUtils {


    @SneakyThrows
    public List<String> list(Path p) {
        try (TarArchiveInputStream is = new TarArchiveInputStream(Files.newInputStream(p, StandardOpenOption.READ))) {
            final ArrayList<String> nn = Lists.newArrayList();
            new Iter(is).forEachRemaining(x -> nn.add(x.getName()));
            return nn;
        }
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

}
