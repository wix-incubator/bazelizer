package tools.jvm.mvn;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.*;

/**
 * Args.
 */
@Accessors(fluent = true)
public class Args implements Iterable<String>  {
    private final LinkedHashSet<String> args = new LinkedHashSet<>();

    @Setter
    @Getter
    private boolean offline;

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
        for (String arg : this) {
            arr[i++] = arg;
        }
        return arr;
    }

    @Override
    public String toString() {
        return Joiner.on(" ").join(args);
    }


    @Override
    public Iterator<String> iterator() {
        return Lists.reverse(Lists.newLinkedList(args)).iterator();
    }
}
