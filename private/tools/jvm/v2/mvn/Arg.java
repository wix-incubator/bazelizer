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
        final String[] flags = cmdLine.split(" ");
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
