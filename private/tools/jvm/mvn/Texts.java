package tools.jvm.mvn;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.cactoos.Scalar;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public final class Texts {

    public static String randomFileName(String pref) {
        return pref + "-" + randomLetters(10)  + "-" + Long.toHexString(System.currentTimeMillis());
    }

    public static String randomLetters() {
        return randomLetters( 10);
    }

    public static String randomLetters(int len) {
        StringBuilder s = new StringBuilder();
        for (int j = 0; j < len; j++) {
            s.append((char) ThreadLocalRandom.current().nextInt('a', 'z'));
        }
        return s.toString();
    }


    @SneakyThrows
    public static URL quietPathToURL(Path path) {
        return path.toUri().toURL();
    }

    @SneakyThrows
    public static URL quietPathToURL(String path) {
        return quietPathToURL(Paths.get(path));
    }

}
