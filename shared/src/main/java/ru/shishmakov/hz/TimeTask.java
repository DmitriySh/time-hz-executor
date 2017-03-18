package ru.shishmakov.hz;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * @author Dmitriy Shishmakov on 16.03.17
 */
public class TimeTask extends HzCallable<Void> implements Comparable<TimeTask> {
    private static final Comparator<TimeTask> TT_COMPARATOR = buildTaskTimeComparator();

    private final long orderId;
    private final long scheduledTime;
    private final Callable<?> task;

    public TimeTask(long orderId, LocalDateTime localDateTime, Callable<?> task) {
        this.orderId = orderId;
        this.scheduledTime = localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        this.task = task;
    }

    public long getOrderId() {
        return orderId;
    }

    public long getScheduledTime() {
        return scheduledTime;
    }

    public Callable<?> getTask() {
        return task;
    }

    @Override
    public Void call() throws Exception {
        task.call();
        return null;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, SHORT_PREFIX_STYLE)
                .append("orderId", orderId)
                .append("scheduledTime", scheduledTime)
                .toString();
    }

    @Override
    public int compareTo(@Nonnull TimeTask other) {
        return TT_COMPARATOR.compare(this, checkNotNull(other, "{} is null", TimeTask.class.getSimpleName()));
    }

    private static Comparator<TimeTask> buildTaskTimeComparator() {
        return Comparator.comparing(TimeTask::getScheduledTime)
                .thenComparing(TimeTask::getOrderId);
    }
}
