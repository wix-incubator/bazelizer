package tools.jvm.mvn;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.spi.ContextAwareBase;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import org.cactoos.Scalar;
import org.slf4j.MDC;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

public class SLF4JConfigurer extends ContextAwareBase implements Configurator {
    private static ToolLogLevel root = ToolLogLevel.OFF;

    public synchronized static ToolLogLevel getLogLevel() {
        return root;
    }

    public synchronized static void setLogLevel(ToolLogLevel l) {
        root = l;
    }


    /**
     * MDC log.
     * @param act act
     */
    @SuppressWarnings("UnusedReturnValue")
    @SneakyThrows
    public static <V> V withMDC(String id, Scalar<V> act) {
        try (Closeable ignored = Optional.ofNullable(id).<Closeable>map(d ->
                MDC.putCloseable("id", d)).orElse(() -> {})) {
            return act.value();
        }
    }

    /**
     * MDC log.
     * @param act act
     */
    @SuppressWarnings("unused")
    public static void withMDC(String id, Runnable act) {
        withMDC(id, () -> {
            act.run();
            return null;
        });
    }

    public static String shortMDC(Path pomFile) {
        final String[] arr = pomFile.toString().split("/");
        final StringJoiner joiner = new StringJoiner("/");
        for (int i = 0; i < arr.length; i++) {
            String s = arr[i];
            if (i > arr.length - 2) {
                joiner.add(s);
            } else {
                String part = s.isEmpty() ? s : s.substring(0,1);
                joiner.add(part);
            }
        }
        return joiner.toString();
    }


    enum ToolLogLevel {
        OFF("off"),

        /**
         * print maven default output + info messages by a tool;
         */
        INFO("info"),

        /**
         * print maven default output + debug messages by a tool (for example generate pom);
         */
        DEBUG("debug"),

        /**
         * print maven debug output + debug messages by a tool;
         */
        TRACE("trace");

        private final String type;

        ToolLogLevel(String type) {
            this.type = type;
        }

        static Optional<ToolLogLevel> find(String name) {
            for (ToolLogLevel l : ToolLogLevel.values()) {
                if (l.type.equalsIgnoreCase(name)) return Optional.of(l);
            }
            return Optional.empty();
        }
    }

    @Override
    public void configure(LoggerContext logCtx) {
        String logLvl = SysProps.logLevel().orElse("OFF");
        ToolLogLevel logLevel = ToolLogLevel.find(logLvl).orElse(ToolLogLevel.OFF);
        System.out.println("tools.jvm.mvn.LogLevel=" + logLevel);
        setLogLevel(logLevel);
        initLoggers(logCtx, logLevel);
    }

    private void initLoggers(LoggerContext logCtx, ToolLogLevel logLevel) {
        PatternLayoutEncoder logEncoder = new PatternLayoutEncoder();
        logEncoder.setContext(logCtx);
        logEncoder.setPattern("[%-5level] %X{id} [class=%logger{0}] - %msg%n");
        logEncoder.start();

        ConsoleAppender<ILoggingEvent> logConsoleAppender = new ConsoleAppender<>();
        logConsoleAppender.setContext(logCtx);
        logConsoleAppender.setName("STDOUT");
        logConsoleAppender.setEncoder(logEncoder);
        logConsoleAppender.start();

        Logger rootLog = logCtx.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLog.addAppender(logConsoleAppender);

        setLoggers(logLevel, rootLog);
    }

    private static void setLoggers(ToolLogLevel logLevel, Logger toolLog) {
        switch (logLevel) {
            case OFF:
            case INFO:
                toolLog.setLevel(Level.INFO);
                break;
            case DEBUG:
            case TRACE:
                toolLog.setLevel(Level.DEBUG);
                break;
        }
    }
}
