package tools.jvm.mvn;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import lombok.Data;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Args.
 */
public class Args  {
    private final LinkedHashSet<String> args = new LinkedHashSet<>();

    /**
     * Add args to set.
     * @param s args
     * @return this
     */
    @SuppressWarnings("UnusedReturnValue")
    public Args append(String...s) {
        args.addAll(Arrays.asList(s));
        return this;
    }

    /**
     * Print args as array.
     * @return array
     */
    public String[] toArray() {
        int i = 0;
        final String[] arr = new String[args.size()];
        for (String arg : args) {
            arr[i++] = arg;
        }
        return arr;
    }

    @Override
    public String toString() {
        return Joiner.on(" ").join(args);
    }

    public List<KW> toKW() {
        final List<String> strings = Lists.newArrayList(toArray());
        final List<KW> vals = Lists.newArrayList();

        for (int i = 0; i < strings.size();) {
            final String n = strings.get(i);

            if (n.startsWith("-")) {
                String key = n;
                while (key.startsWith("-")) {
                    key = key.substring(1);
                }
                String val = Iterables.get(strings, ++i, null);
                KW kw = new KW(key, val);
                vals.add(kw);
            } else {
                vals.add(new KW(n, null));
            }

            i++;
        }
        return vals;
    }

    @Data
    public static class KW {
        final String key;
        final String value;
    }
}
