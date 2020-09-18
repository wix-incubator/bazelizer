package tools.jvm.mvn;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import lombok.Data;

import java.util.*;

/**
 * Args.
 */
public class Args implements Iterable<String>  {
    private final LinkedHashSet<String> args = new LinkedHashSet<>();

    public Args() { }


    public Args(Args col) {
        for (String s : col) {
            append(s);
        }
    }

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

    public List<KeyValue> getKeyValues() {
        final String[] strings = toArray();
        final List<KeyValue> vals = Lists.newArrayList();
        for (int i = 0; i < strings.length;) {
            final String n = strings[i];
            if (n.startsWith("-")) {
                String key = n;
                while (key.startsWith("-")) {
                    key = key.substring(1);
                }
                String val = strings[++i];
                KeyValue kw = new KeyValue(key, val);
                vals.add(kw);
            } else {
                vals.add(new KeyValue(n, null));
            }
            i++;
        }
        return vals;
    }

    @Data
    public static class KeyValue {
        final String key;
        final String value;
    }


    @Override
    public Iterator<String> iterator() {
        return Collections.unmodifiableCollection(args).iterator();
    }
}
