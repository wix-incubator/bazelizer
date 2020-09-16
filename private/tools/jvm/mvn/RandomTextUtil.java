package tools.jvm.mvn;

import lombok.experimental.UtilityClass;

import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public final class RandomTextUtil {

    public static String randomStr(String pref) {
        return randomStr(pref, 10);
    }

    public static String randomStr(String pref, int len) {
        StringBuilder s = new StringBuilder();
        for (int j = 0; j < len; j++) {
            s.append((char) ThreadLocalRandom.current().nextInt('a', 'z'));
        }
        return pref + s.toString();
    }
}
