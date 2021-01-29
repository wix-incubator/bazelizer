package tools.jvm.v2.mvn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@CommandLine.Command(subcommands = {
        CmdRepository.class,
        CmdBuild.class
})
public class Main {
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Path.class, (JsonDeserializer<Path>) (json, typeOfT, context) -> {
                final String asString = json.getAsString();
                return Paths.get(asString);
            }).create();


    public static void main(String[] args) {
        LocalDateTime from = LocalDateTime.now();
        int exitCode = new CommandLine(new Main()).execute(args);
        log.info(">> {}", exitCode == 0 ? "DONE" : "FAIL");
        LocalDateTime to = LocalDateTime.now();
        log.info(">>  time elapsed: {}s", Duration.between(from, to).getSeconds());
        System.exit(exitCode);
    }
}
