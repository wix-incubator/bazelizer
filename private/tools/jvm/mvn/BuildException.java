package tools.jvm.mvn;

public class BuildException extends RuntimeException {
    public BuildException(Throwable cause) {
        super(cause);
    }
    public BuildException(String cause) {
        super(cause);
    }

    public BuildException(String message, Throwable cause) {
        super(message, cause);
    }
}
