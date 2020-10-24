package tools.jvm.mvn;

import com.google.common.hash.Hashing;
import com.google.devtools.build.runfiles.Runfiles;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class SysProps {

    public static final String PREF = "tools.jvm.mvn.";


    /**
     * Hex of invoking label name.
     * @return hex
     */
    @SuppressWarnings("UnstableApiUsage")
    public static Optional<String> labelHex() {
        return Optional.ofNullable(System.getProperty(PREF + "BazelLabelName"))
                .map(name -> Hashing.murmur3_32().hashString(name, StandardCharsets.UTF_8).toString());
    }

    /**
     * Log level.
     */
    public static Optional<String> logLevel() {
        return Optional.ofNullable(System.getProperty(PREF + "LogLevel"));
    }

    /**
     * Current workspace name.
     * @return name
     */
    public static Optional<String> workspace() {
        final Optional<String> optional = Optional.ofNullable(System.getProperty("maven.bin.workspace"));
        return optional.isPresent() ? optional : Optional.ofNullable(System.getProperty(PREF + "Ws"));
    }

    /**
     * Linked user cache directory name, inside runfiles
     * @return cache in runfiles
     */
    public static String getSysPropMavenTool() {
        final String key = PREF + "MavenBin";
        return Optional.ofNullable(System.getProperty(key)).map(SysProps::getMavenTool)
                .orElseThrow(() ->
                        new IllegalStateException("property no specified " + key));
    }

    /**
     * Linked user cache directory name, inside runfiles
     * @return cache in runfiles
     */
    public static String getMavenTool(String runfilesPath) {
        final Runfiles runfiles = runfiles();
        return runfiles.rlocation(runfilesPath);
    }

    /**
     * Bazel runfiles.
     * @return runfiles
     * @throws IOException Runfiles not available.
     */
    @SneakyThrows
    public synchronized static Runfiles runfiles()  {
        if (_sRunfiles == null) {
            _sRunfiles = Runfiles.create();
        }
        return _sRunfiles;
    }

    private static Runfiles _sRunfiles;
}
