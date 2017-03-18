package ru.shishmakov.core;

import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.concurrent.Threads;
import ru.shishmakov.config.TimeConfig;
import ru.shishmakov.hz.HzObjects;
import ru.shishmakov.hz.TimeTask;
import ru.shishmakov.util.QueueUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Dmitriy Shishmakov on 16.03.17
 */
@Singleton
public class FirstLevelWatcher {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private TimeConfig timeConfig;
    @Inject
    private HzObjects hzObjects;
    @Inject
    @Named("timeQueue.firstLevel")
    public BlockingQueue<TimeTask> queueFirstLevel;

    private IMap<Long, TimeTask> mapFirstLevel;
    private final AtomicBoolean FL_WATCHER_STATE = new AtomicBoolean(true);

    public void start() {
        logger.info("First level watcher started");
        this.mapFirstLevel = hzObjects.getFirstLevelMap();
        try {
            while (FL_WATCHER_STATE.get() && !Thread.currentThread().isInterrupted()) {
                getHotFirstLevelTasks().forEach(t -> {
                    logger.debug("<--  take FL task \'{}\'", t);
                    QueueUtils.offer(queueFirstLevel, t);
                    mapFirstLevel.removeAsync(t.getOrderId());
                    logger.debug("-->  put FL task \'{}\' : firstLevel PriorityBlockingQueue", t);
                });
                Threads.sleepWithInterrupted(timeConfig.scanIntervalMs(), MILLISECONDS);
            }
        } catch (Exception e) {
            logger.error("Error in time of processing", e);
        } finally {
            FL_WATCHER_STATE.compareAndSet(true, false);
        }
    }

    public void stop() {
        FL_WATCHER_STATE.compareAndSet(true, false);
        logger.info("First level watcher stopped");
    }

    private Collection<TimeTask> getHotFirstLevelTasks() {
        final long upperBound = timeConfig.hotTaskUpperBoundMs();
        final long now = hzObjects.getClusterTime();
        final Set<Long> localHotKeys = new HashSet<>(mapFirstLevel
                .localKeySet(Predicates.lessEqual("scheduledTime", now + upperBound)));
//                .localKeySet((Predicate<Long, TimeTask>) e -> e.getValue().getScheduledTime() - now <= upperBound));
        return localHotKeys.isEmpty()
                ? Collections.emptyList()
                : mapFirstLevel.values((Predicate<Long, TimeTask>) e -> localHotKeys.contains(e.getKey()));
    }
}
