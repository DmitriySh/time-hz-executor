package ru.shishmakov.concurrent;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.LongAdder;

/**
 * @author Dmitriy Shishmakov on 11.03.17
 */
public class TaskRejectedHandler implements RejectedExecutionHandler {
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
