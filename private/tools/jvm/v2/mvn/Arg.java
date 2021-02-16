package tools.jvm.v2.mvn;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import lombok.Data;
import picocli.CommandLine;

import java.util.Arrays;
import java.util.List;


@Data
public class Arg {
    @CommandLine.Option(names = {"-P", "--active-profiles"}, split = ",")
    private List<String> profiles = Lists.newArrayList();
    @CommandLine.Option(names = {"--goals"}, split = ",")
    private List<String> goals = Lists.newArrayList();

    public Arg(String cmdLine) {
        this(cmdLine != null ? format(cmdLine) : new String[0]);
    }

    private static String[] format(String argsLine) {
        String line = argsLine;
        if (line.startsWith("'") || line.startsWith("\"")) {
            line = line.substring(1);
        }
        if (line.endsWith("'") || line.endsWith("\"")) {
            line = line.substring(0, line.length() - 1);
        }
        return line.split(" ");
    }

    public Arg(List<String> cmdLine) {
        this(cmdLine.toArray(new String[0]));
    }

    public Arg(String[] flags) {
        try {
            final CommandLine.ParseResult result = new CommandLine(this).parseArgs(flags);
            Preconditions.checkState(!result.isUsageHelpRequested());
            Preconditions.checkState(!result.isVersionHelpRequested());
        } catch (CommandLine.ParameterException e) {
            throw new IllegalArgumentException("[Arg] flags " + Arrays.toString(flags)   + " not valid: "
                    + e.getMessage() + "\n correct flags: "
                    + e.getCommandLine().getHelp().synopsis(0)
            );
        }
    }

}
