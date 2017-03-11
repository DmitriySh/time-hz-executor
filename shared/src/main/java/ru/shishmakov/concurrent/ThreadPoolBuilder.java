package ru.shishmakov.concurrent;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.*;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Dmitriy Shishmakov on 11.03.17
 */
public class ThreadPoolBuilder {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int DEFAULT_QUEUE_CAPACITY = 4096;

    private final TaskRejectedHandler rejectedHandler;
    private final ThreadFactoryBuilder threadFactory;
    private final String name;

    private int min;
    private int max;
    private BlockingQueue<Runnable> queue;
    private long idleTime;
    private TimeUnit idleUnit;

    private ThreadPoolBuilder(String name) {
        this.name = name;
        this.rejectedHandler = new TaskRejectedHandler(new AbortPolicy());
        this.threadFactory = defaultThreadFactory(name);
        this.queue = new ArrayBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
        this.min = 1;
        this.max = Math.max(min, Runtime.getRuntime().availableProcessors() * 4);
        this.idleTime = 60;
        this.idleUnit = SECONDS;
    }

    public static ThreadPoolBuilder pool(String name) {
        return new ThreadPoolBuilder(checkNotNull(name, "pool name should not be null"));
    }

    public ThreadPoolBuilder withIdleTime(long idleTime, TimeUnit unit) {
        checkArgument(idleTime > 0, "idle time of thread should be positive value: %s", idleTime);
        this.idleUnit = checkNotNull(unit, "time unit should not be null");
        this.idleTime = idleTime;
        return this;
    }

    public ThreadPoolBuilder withDaemonThreads(boolean daemon) {
        threadFactory.setDaemon(daemon);
        return this;
    }

    public ThreadPoolBuilder withPriorityThreads(int priority) {
        threadFactory.setPriority(priority);
        return this;
    }

    public ThreadPoolBuilder withThreads(int min, int max) {
        return withMin(min).withMax(max);
    }


    public ThreadPoolBuilder withMin(int threads) {
        checkArgument(threads >= 0, "thread count should not be negative value: %s", threads);
        return (this.min = threads) > max ? withMax(threads) : this;
    }

    public ThreadPoolBuilder withMax(int threads) {
        checkArgument(threads >= 0, "threads should not be negative value: %s", threads);
        return (this.max = threads) < min ? withMin(threads) : this;
    }

    public ThreadPoolBuilder withQueue(BlockingQueue<Runnable> queue) {
        this.queue = checkNotNull(queue, "queue should not be null");
        return this;
    }

    public ThreadPoolBuilder withArrayQueue(int capacity) {
        checkArgument(capacity > 0, "capacity should be positive value: %s", capacity);
        return withQueue(new ArrayBlockingQueue<>(capacity));
    }

    public ThreadPoolBuilder withLinkedQueue(int capacity) {
        checkArgument(capacity > 0, "capacity should be positive value: %s", capacity);
        return withQueue(new LinkedBlockingQueue<>(capacity));
    }

    public ThreadPoolBuilder withSyncQueue() {
        return withQueue(new SynchronousQueue<>());
    }

    public ThreadPoolExecutor build() {
        logger.info("create thread pool: {}, threads [{}..{}], idleTime: {} {}, queue: {} {} ",
                name, min, max, idleTime, idleUnit, queue.getClass().getSimpleName(), queue);
        return new ThreadPoolExecutor(min, max, idleTime, idleUnit, queue, threadFactory.build(), rejectedHandler);
    }

    private static ThreadFactoryBuilder defaultThreadFactory(String name) {
        ThreadFactoryBuilder factory = new ThreadFactoryBuilder();
        factory.setNameFormat(name + "-%d");
        factory.setUncaughtExceptionHandler((t, e) -> logger.warn("thread pool: " + name + " has unhandled exception", e));
        return factory;
    }
}
