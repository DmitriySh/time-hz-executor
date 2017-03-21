package ru.shishmakov.hz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Callable wrapper for executing
 *
 * @author Dmitriy Shishmakov on 20.03.17
 */
public class MessageTask extends HzCallable<Void> {
    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final long scheduledTime;
    private final String message;

    public MessageTask(String message, LocalDateTime localDateTime) {
        this.message = message;
        this.scheduledTime = localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    @Override
    public Void call() throws Exception {
        logger.info("Run task; time: {}, message: {}",
                LocalDateTime.ofInstant(Instant.ofEpochMilli(scheduledTime), ZoneId.of("UTC")),
                message);
        return null;
    }
}
