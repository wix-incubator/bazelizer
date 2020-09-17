package tools.jvm.mvn;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.*;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.*;
import ch.qos.logback.core.spi.ContextAwareBase;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class SLF4JConfigurer extends ContextAwareBase implements Configurator {

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

    static void logLevel(ToolLogLevel lvl) {
        System.out.println("Log level " + lvl);
        setLoggers(lvl);
    }


    @Override
    public void configure(LoggerContext logCtx) {
        String logLvl = System.getProperty("tools.jvm.mvn.LogLevel");
        ToolLogLevel logLevel = ToolLogLevel.find(logLvl).orElse(ToolLogLevel.OFF);
        initLoggers(logCtx, logLevel);
    }

    private void initLoggers(LoggerContext logCtx, ToolLogLevel logLevel) {
        PatternLayoutEncoder logEncoder = new PatternLayoutEncoder();
        logEncoder.setContext(logCtx);
        logEncoder.setPattern("[%level] %msg%n");
        logEncoder.start();
        ConsoleAppender<ILoggingEvent> logConsoleAppender = new ConsoleAppender<>();
        logConsoleAppender.setContext(logCtx);
        logConsoleAppender.setName("STDOUT");
        logConsoleAppender.setEncoder(logEncoder);
        logConsoleAppender.start();

        PatternLayoutEncoder logEncoder2 = new PatternLayoutEncoder();
        logEncoder2.setContext(logCtx);
        logEncoder2.setPattern("%d{HH:mm:ss.SSS} %-5level [class=%logger{0}] - %msg%n");
        logEncoder2.start();
        ConsoleAppender<ILoggingEvent> logConsoleAppender2 = new ConsoleAppender<>();
        logConsoleAppender2.setContext(logCtx);
        logConsoleAppender2.setName("STDOUT2");
        logConsoleAppender2.setEncoder(logEncoder2);
        logConsoleAppender2.start();

        Logger rootLog = logCtx.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLog.setAdditive(false);
        rootLog.addAppender(logConsoleAppender);

        Logger toolLog = logCtx.getLogger("tools.jvm.mvn");
        toolLog.setAdditive(false);
        toolLog.addAppender(logConsoleAppender2);

        setLoggers(logLevel, rootLog, toolLog);
    }

    private static void setLoggers(ToolLogLevel logLevel) {
        final LoggerContext logCtx = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLog = logCtx.getLogger(Logger.ROOT_LOGGER_NAME);
        Logger toolLog = logCtx.getLogger("tools.jvm.mvn");
        setLoggers(logLevel, rootLog, toolLog);
    }

    private static void setLoggers(ToolLogLevel logLevel, Logger rootLog, Logger toolLog) {
        switch (logLevel) {
            case OFF:
                rootLog.setLevel(Level.ERROR);
                toolLog.setLevel(Level.INFO);
                break;
            case INFO:
                rootLog.setLevel(Level.INFO);
                toolLog.setLevel(Level.INFO);
                break;
            case DEBUG:
                rootLog.setLevel(Level.INFO);
                toolLog.setLevel(Level.DEBUG);
                break;
            case TRACE:
                rootLog.setLevel(Level.DEBUG);
                toolLog.setLevel(Level.DEBUG);
                break;
        }
    }
}
