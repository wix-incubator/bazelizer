package tools.jvm.mvn;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.*;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.*;
import ch.qos.logback.core.spi.ContextAwareBase;
import org.slf4j.LoggerFactory;

public class SLF4JConfigurer extends ContextAwareBase implements Configurator {

    public SLF4JConfigurer() {
        super();
    }

    @Override
    public void configure(LoggerContext logCtx) {
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


        Logger log = logCtx.getLogger(Logger.ROOT_LOGGER_NAME);
        log.setAdditive(false);
        log.setLevel(Level.INFO);
        log.addAppender(logConsoleAppender);

        Logger log2 = logCtx.getLogger("tools.jvm.mvn");
        log2.setAdditive(false);
        log2.setLevel(Level.INFO);
        log2.addAppender(logConsoleAppender2);
    }
}
