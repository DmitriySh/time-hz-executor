package ru.shishmakov.concurrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.*;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.atomic.LongAdder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Dmitriy Shishmakov on 11.03.17
 */
public class ThreadPools {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static ThreadPoolExecutor buildThreadPool(String name, int min, int max) {
        return ThreadPoolBuilder.pool(name)
                .withThreads(min, max)
                .withIdleTime(60, SECONDS)
                .build();
    }

    public static class ThreadPoolBuilder {
        private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
        private static final int DEFAULT_QUEUE_CAPACITY = 4096;
        protected TaskRejectedHandler rejectedHandler = new TaskRejectedHandler(new AbortPolicy());

        private final String name;

        private int min = 1;
        private int max = Math.max(min, Runtime.getRuntime().availableProcessors() * 4);
        private BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(DEFAULT_QUEUE_CAPACITY);
        private long idleTime;
        private TimeUnit unit;

        private ThreadPoolBuilder(String name) {
            this.name = name;
        }

        public static ThreadPoolBuilder pool(String name) {
            return new ThreadPoolBuilder(checkNotNull(name, "pool name should not be null"));
        }

        public ThreadPoolBuilder withIdleTime(long idleTime, TimeUnit unit) {
            checkArgument(idleTime > 0, "idle time of thread should be positive value: %s", idleTime);
            this.unit = checkNotNull(unit, "time unit should not be null");
            this.idleTime = idleTime;
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
            logger.debug("create thread pool: {}, threads [{}..{}], idleTime: {} {}, queue: {} {} ",
                    name, min, max, idleTime, unit, queue.getClass(), queue);
            return new ThreadPoolExecutor(min, max, idleTime, unit, queue, tf.build(), rejectedHandler);
        }
    }

    public static class TaskRejectedHandler implements RejectedExecutionHandler {
        private final LongAdder count = new LongAdder();
        private final RejectedExecutionHandler handler;

        public TaskRejectedHandler(RejectedExecutionHandler handler) {
            this.handler = handler;
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            count.increment();
            handler.rejectedExecution(r, executor);
        }

        public long rejected(boolean needReset) {
            return needReset ? count.sumThenReset() : count.longValue();
        }
    }

}
