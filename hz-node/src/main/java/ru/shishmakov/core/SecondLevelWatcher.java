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
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Dmitriy Shishmakov on 16.03.17
 */
@Singleton
public class SecondLevelWatcher {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private TimeConfig timeConfig;
    @Inject
    private HzObjects hzObjects;
    @Inject
    @Named("timeQueue.secondLevel.1")
    public BlockingQueue<TimeTask> queueSecondLevel1;
    @Inject
    @Named("timeQueue.secondLevel.2")
    public BlockingQueue<TimeTask> queueSecondLevel2;
    @Inject
    @Named("timeQueue.secondLevel.3")
    public BlockingQueue<TimeTask> queueSecondLevel3;

    private IMap<Long, TimeTask> mapSecondLevel;
    private final AtomicBoolean SL_WATCHER_STATE = new AtomicBoolean(true);

    public void start() {
        logger.info("Second level watcher started");
        this.mapSecondLevel = hzObjects.getSecondLevelMap();
        try {
            while (SL_WATCHER_STATE.get() && !Thread.currentThread().isInterrupted()) {
                Map<Long, List<TimeTask>> tasks = getHotSecondLevelTasks();
                tasks.
                for (TimeTask task : getHotFirstLevelTasks()) {
                    logger.debug("<--  take FL task \'{}\'", task);

                    QueueUtils.offer(queueFirstLevel, task);
                    logger.debug("-->  put FL task \'{}\' : firstLevel PriorityBlockingQueue", task);

                    Threads.sleepWithInterrupted(timeConfig.scanIntervalMs(), MILLISECONDS);
                }
            }
        } catch (Exception e) {
            logger.error("Error in time of processing", e);
        } finally {
            SL_WATCHER_STATE.compareAndSet(true, false);
        }
    }

    public void stop() {
        SL_WATCHER_STATE.compareAndSet(true, false);
        logger.info("Second level watcher stopped");
    }

    private Map<Long, List<TimeTask>> getHotSecondLevelTasks() {
        final long upperBound = timeConfig.hotTaskUpperBoundMs();
        final long now = hzObjects.getClusterTime();
        final Set<Long> localHotKeys = new HashSet<>(mapSecondLevel
                .localKeySet(Predicates.lessEqual("scheduledTime", now + upperBound)));
//                .localKeySet((Predicate<Long, TimeTask>) e -> e.getValue().getScheduledTime() - now <= upperBound));
        return (localHotKeys.isEmpty()
                ? Collections.<TimeTask>emptyList()
                : mapSecondLevel.values((Predicate<Long, TimeTask>) e -> localHotKeys.contains(e.getKey())))
                .stream().sorted().collect(Collectors.groupingBy(TimeTask::getScheduledTime, Collectors.toList()));
    }
}


















