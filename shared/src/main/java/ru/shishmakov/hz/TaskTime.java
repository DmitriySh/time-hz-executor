package ru.shishmakov.hz;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * @author <a href="mailto:d.shishmakov@corp.nekki.ru">Shishmakov Dmitriy</a>
 */
public class TaskTime extends HzCallable<Void> implements Comparable<TaskTime> {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    static final Comparator<TaskTime> TT_COMPARATOR = buildTaskTimeComparator();


    private final long orderId;
    private final long scheduledTime;
    private final Callable<?> task;

    public TaskTime(long orderId, LocalDateTime localDateTime, Callable<?> task) {
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
    public int compareTo(@Nonnull TaskTime other) {
        return TT_COMPARATOR.compare(this, checkNotNull(other, "TaskTime is null"));
    }

    private static Comparator<TaskTime> buildTaskTimeComparator() {
        return Comparator.comparing(TaskTime::getScheduledTime)
                .thenComparing(TaskTime::getOrderId);
    }
}
