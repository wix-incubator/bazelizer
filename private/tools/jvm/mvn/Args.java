package tools.jvm.mvn;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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
    private final ArrayDeque<String> args = new ArrayDeque<>();

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
        for (String ss : s) {
            args.addLast(ss);
        }
        args.addAll(Arrays.asList(s));
        return this;
    }

    /**
     * Add args to set.
     * @param s args
     * @return this
     */
    @SuppressWarnings("UnusedReturnValue")
    public Args prepend(String...s) {
        for (String ss : s) {
            args.addFirst(ss);
        }
        return this;
    }

    /**
     * Print args as array.
     * @return array
     */
    public String[] toArray() {
        int i = 0;
        final LinkedHashSet<String> args = Sets.newLinkedHashSet(this.args);
        final String[] arr = new String[args.size()];
        for (String arg : args) {
            arr[i++] = arg;
        }
        return arr;
    }

    @Override
    public String toString() {
        return Joiner.on(" ").join(this);
    }


    @Override
    public Iterator<String> iterator() {
        return Sets.newLinkedHashSet(args).iterator();
    }
}
