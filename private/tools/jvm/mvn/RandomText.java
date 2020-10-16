package tools.jvm.mvn;

import lombok.experimental.UtilityClass;

import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public final class RandomText {

    public static String randomFileName(String pref) {
        return pref + "-" + randomStr(10)  + "-" + Long.toHexString(System.currentTimeMillis());
    }

    public static String randomStr() {
        return randomStr( 10);
    }

    public static String randomStr(int len) {
        StringBuilder s = new StringBuilder();
        for (int j = 0; j < len; j++) {
            s.append((char) ThreadLocalRandom.current().nextInt('a', 'z'));
        }
        return s.toString();
    }
}
