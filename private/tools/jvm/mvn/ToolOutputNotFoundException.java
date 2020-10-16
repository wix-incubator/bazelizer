package tools.jvm.mvn;

import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class ToolOutputNotFoundException extends RuntimeException {

    public ToolOutputNotFoundException(Object src, Path target, NoSuchFileException e) {
        super("Source " + src + " not found within: [\n" + exists(target) + "\n]", e);
    }

    @SneakyThrows
    private static String exists(Path target)  {
        return Files.walk(target, 1)
                .limit(10).map(Path::toString).collect(Collectors.joining("\n"));
    }
}
