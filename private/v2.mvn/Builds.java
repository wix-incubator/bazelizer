package tools.jvm.v2.mvn;

import com.google.common.hash.Hashing;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import tools.jvm.mvn.Args;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public abstract class Builds {
    private final Map<Path,Build> buildMap = new HashMap<>();


    public static Builds read(Manifest manifest) {
        final List<BuildDTO> items = manifest.items(BuildDTO.class);

        return new Builds() {
        };
    }

    @Data
    public static class Build {
        private final Path pom;
        private final Build parent;
        private final List<Build> deps = new ArrayList<>();
    }




    /**
     * Pom definition.
     */
    @Data
    public static class BuildDTO {
        @SerializedName("file")
        private Path file;
        @SerializedName("parent_file")
        private Path parentFile;
        @SerializedName("flags_line")
        private List<String> flags;
    }




}
