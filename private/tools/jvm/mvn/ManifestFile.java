package tools.jvm.mvn;

import com.google.common.collect.Iterators;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import lombok.SneakyThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class ManifestFile implements Iterable<String> {
    public static final String WRAP = "'";

    private final CharSource source;

    public ManifestFile(Path source) {
        this(Files.asByteSource(source.toFile()).asCharSource(StandardCharsets.UTF_8));
    }

    public ManifestFile(CharSource source) {
        this.source = source;
    }

    @SuppressWarnings({"ConstantConditions", "NullableProblems"})
    @SneakyThrows
    @Override
    public Iterator<String> iterator() {
        return Iterators.transform(source.readLines().iterator(), p -> {
            String base = p.trim();
            if (base.startsWith(WRAP)) {
                base = base.substring(1);
            }
            if (base.endsWith(WRAP)) {
                base = base.substring(0, base.length() - 1);
            }
            return base;
        });
    }
}
