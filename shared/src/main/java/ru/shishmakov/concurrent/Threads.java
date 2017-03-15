package ru.shishmakov.concurrent;

import com.google.common.util.concurrent.Uninterruptibles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * @author Dmitriy Shishmakov on 13.03.17
 */
public final class Threads {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final long STOP_TIMEOUT_SEC = 10;

    /**
     * Modified version of the method {@link Uninterruptibles#sleepUninterruptibly(long, TimeUnit)}
     * without interrupted thread
     */
    public static void sleepWithoutInterrupted(long timeout, TimeUnit unit) {
        long remainingNanos = unit.toNanos(timeout);
        final long end = System.nanoTime() + remainingNanos;
        while (true) {
            try {
                // TimeUnit.sleep() treats negative timeouts just like zero.
                NANOSECONDS.sleep(remainingNanos);
                return;
            } catch (InterruptedException e) {
                remainingNanos = end - System.nanoTime();
            }
        }
    }

    public static void sleepWithInterrupted(long timeout, TimeUnit unit) {
        Uninterruptibles.sleepUninterruptibly(timeout, unit);
    }

    public static void assignThreadHook(Runnable task, String name) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.debug("Thread: {} was interrupted by hook", Thread.currentThread());
            task.run();
        }, name));
    }
}
