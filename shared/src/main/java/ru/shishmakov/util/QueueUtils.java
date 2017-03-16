package ru.shishmakov.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author <a href="mailto:d.shishmakov@corp.nekki.ru">Shishmakov Dmitriy</a>
 */
public final class QueueUtils {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int TIMES_DEFAULT = 11;
    private static final int DELAY_DEFAULT = 20;

    public static <T> Optional<T> poll(BlockingQueue<T> queue) {
        return poll(queue, TIMES_DEFAULT, DELAY_DEFAULT, MILLISECONDS);
    }

    public static <T> boolean offer(BlockingQueue<T> queue, T task) {
        return offer(queue, task, TIMES_DEFAULT, DELAY_DEFAULT, MILLISECONDS);
    }

    public static <T> Optional<T> poll(BlockingQueue<T> queue, int times, int delay, TimeUnit unit) {
        try {
            T task = null;
            while (times-- > 0 && (task = queue.poll(delay, unit)) == null) {
                logger.debug("effort: {} X--- element is absent; delay: {}", times, delay);
            }
            logger.debug("<--- take element: {}", (task == null) ? null : task.getClass().getSimpleName());
            return Optional.ofNullable(task);
        } catch (Exception e) {
            logger.error("Queue poll exception ...", e);
            return Optional.empty();
        }
    }

    public static <T> boolean offer(BlockingQueue<T> queue, T task, int times, int delay, TimeUnit unit) {
        try {
            boolean success = false;
            while (--times > 0 && !(success = queue.offer(task, delay, unit))) {
                logger.debug("effort: {} ---X reject element: {}; delay: {}", times, task.getClass().getSimpleName());
            }
            if (success) logger.debug("---> insert element: {}", task.getClass().getSimpleName());
            return success;
        } catch (Exception e) {
            logger.error("Queue offer exception ...", e);
        }
        return false;
    }
}
