package tools.jvm.v2.mvn;

import com.google.common.collect.Lists;
import lombok.ToString;

import java.util.List;
import java.util.regex.Pattern;

public interface XPathTypo {
    String apply(String query);

    @ToString
    static class Prefix implements XPathTypo {
        @SuppressWarnings("RegExpRedundantEscape")
        private final static Pattern ATTRS = Pattern.compile("^\\w+(\\[(.*?)\\])");

        /**
         * Ctor.
         * @param prefix prefix
         */
        public Prefix(String prefix) {
            this.prefix = prefix;
            this.fmt = prefix + ":%s";
        }

        private final String prefix;
        private final String fmt;

        @SuppressWarnings("DuplicatedCode")
        @Override
        public String apply(String query) {
            final List<String> tokens = Lists.newArrayList();
            String startOf = "";
            for (String token : query.split("/")) {
                if (token.isEmpty()) {
                    //noinspection StringConcatenationInLoop
                    startOf += "/";
                    continue;
                }
                if (token.contains("()")) {
                    tokens.add(token);
                    continue;
                }
                if (ATTRS.matcher(token).matches()) {
                    tokens.add(String.format(fmt, token));
                    continue;
                }
                if (!isAlpha(token)) {
                    tokens.add(token);
                    continue;
                }
                tokens.add(String.format(fmt, token));
            }
            return startOf + String.join("/", tokens);
        }

        private static boolean isAlpha(String str) {
            int sz = str.length();
            for (int i = 0; i < sz; i++) {
                if (!Character.isLetter(str.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
    }
}
