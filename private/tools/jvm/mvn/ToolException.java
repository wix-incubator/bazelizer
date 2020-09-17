package tools.jvm.mvn;

public class ToolException extends RuntimeException {
    public ToolException(Throwable cause) {
        super(cause);
    }
    public ToolException(String cause) {
        super(cause);
    }

    public ToolException(String message, Throwable cause) {
        super(message, cause);
    }
}
