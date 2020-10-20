package tools.jvm.mvn;

import org.apache.maven.shared.invoker.InvocationResult;

public class ToolMavenInvocationException extends ToolException {

    public ToolMavenInvocationException(InvocationResult r) {
        super("Maven invocation failed, Exit code: " + r.getExitCode(), r.getExecutionException());
    }
}
