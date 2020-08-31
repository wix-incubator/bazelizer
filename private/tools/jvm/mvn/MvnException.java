package tools.jvm.mvn;

public class MvnException extends RuntimeException {
    public MvnException(Throwable cause) {
        super(cause);
    }
    public MvnException(String cause) {
        super(cause);
    }

    public MvnException(String message, Throwable cause) {
        super(message, cause);
    }
}
