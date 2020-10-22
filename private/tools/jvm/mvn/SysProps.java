package tools.jvm.mvn;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Supplier;

public class SysProps {

    @SuppressWarnings("UnstableApiUsage")
    public static Optional<String> labelHex() {
        return Optional.ofNullable(System.getProperty("tools.jvm.mvn.BazelLabelName"))
                .map(name -> Hashing.murmur3_32().hashString(name, StandardCharsets.UTF_8).toString());
    }

    public static Optional<String> label() {
        return Optional.ofNullable(System.getProperty("tools.jvm.mvn.BazelLabelName"));
    }

    public static Optional<String> logLevel() {
        return Optional.ofNullable(System.getProperty("tools.jvm.mvn.LogLevel"));
    }
}
