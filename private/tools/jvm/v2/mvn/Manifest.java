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
            return s.map(p -> formatLine(p.trim())).collect(Collectors.toList());
        }
    }

    public static String formatLine(String argsLine) {
        String line = argsLine;
        if (line.startsWith("'") || line.startsWith("\"")) {
            line = line.substring(1);
        }
        if (line.endsWith("'") || line.endsWith("\"")) {
            line = line.substring(0, line.length() - 1);
        }
        return line;
    }
}
