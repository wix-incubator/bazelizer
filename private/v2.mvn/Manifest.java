package tools.jvm.v2.mvn;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
public class Manifest {
    public static final String WRAP = "'";

    private final Path text;

    public <T> List<T> items(Class<? extends T> clz) {
        return lines().stream().map(json -> Main.GSON.fromJson(json, clz)).collect(Collectors.toList());
    }


    @SneakyThrows
    public List<String> lines() {
        try (Stream<String> s = Files.lines(text)) {
            return s.map(p -> {
                String base = p.trim();
                if (base.startsWith(WRAP)) {
                    base = base.substring(1);
                }
                if (base.endsWith(WRAP)) {
                    base = base.substring(0, base.length() - 1);
                }
                return base;
            }).collect(Collectors.toList());
        }
    }
}
