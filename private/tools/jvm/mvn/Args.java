package tools.jvm.mvn;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import picocli.CommandLine;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Args.
 */
@Accessors(fluent = true)
public class Args  {
    private final List<String> goals = Lists.newArrayList();
    private final List<String> profiles = Lists.newArrayList();


    private static class CmdFlags {
        @CommandLine.Option(names = {"-P", "--active-profiles"}, split = ",")
        public List<String> profiles = Lists.newArrayList();

        @CommandLine.Option(names = {"-go", "--goals"}, split = ",")
        public List<String> goals = Lists.newArrayList();
    }

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
        goals.addAll(Arrays.asList(s));
        return this;
    }

    /**
     * Append from command line string.
     * @param cmdLine line
     * @return args
     */
    @SuppressWarnings("UnusedReturnValue")
    public Args parseCommandLine(String cmdLine) {
        final String[] flags = cmdLine.split(" ");
        final CmdFlags params = resolve(flags);

        this.goals.addAll(params.goals);
        this.profiles.addAll(params.profiles);
        return this;
    }

    private CmdFlags resolve(String[] flags) {
        final CmdFlags params;
        try {
            params = new CmdFlags();
            final CommandLine.ParseResult result = new CommandLine(params).parseArgs(flags);
            Preconditions.checkState(!result.isUsageHelpRequested());
            Preconditions.checkState(!result.isVersionHelpRequested());
        } catch (CommandLine.ParameterException e) {
            throw new IllegalStateException("Args: flags not valid: " + e.getMessage() + ". " + Arrays.toString(flags));
        }
        return params;
    }

    public InvocationRequest toInvocationRequest() {
        final DefaultInvocationRequest request = new DefaultInvocationRequest();
        request.setGoals(Lists.newArrayList(Sets.newLinkedHashSet(goals)));
        request.setProfiles(Lists.newArrayList(Sets.newLinkedHashSet(profiles)));
        return request;
    }

    @Override
    public String toString() {
        return "Args{"
                + goals.stream().map(s -> "--goals=" + s).collect(Collectors.joining(" ")) + " "
                + profiles.stream().map(s -> "--active-profiles=" + s).collect(Collectors.joining(" ")) + "}";

    }
}
