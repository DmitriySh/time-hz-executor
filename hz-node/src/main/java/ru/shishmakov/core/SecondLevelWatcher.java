package ru.shishmakov.core;

import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.shishmakov.concurrent.Threads.sleepInterrupted;

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
    @Named("timeQueue.queueSecondLevel")
    public List<BlockingQueue<TimeTask>> queueSecondLevel;

    private IMap<Long, TimeTask> mapSecondLevel;
    private final AtomicBoolean SL_WATCHER_STATE = new AtomicBoolean(true);
    private final CountDownLatch awaitStop = new CountDownLatch(1);

    public void start() {
        this.mapSecondLevel = hzObjects.getSecondLevelMap();
        try {
            logger.info("Second level watcher started");
            while (SL_WATCHER_STATE.get() && !Thread.currentThread().isInterrupted()) {
                final long now = hzObjects.getClusterTime();
                getHotSecondLevelTasks().forEach((time, list) -> {
                    Collections.sort(list);
                    list.forEach(t -> {
                        logger.debug("<--  take SL task \'{}\'; now: {}, scheduledTime: {}, delta: {}",
                                t, now, t.getScheduledTime(), t.getScheduledTime() - now);
                        if (QueueUtils.offer(nextQueue(), t)) {
                            mapSecondLevel.removeAsync(t.getOrderId());
                            logger.debug("-->  put SL task \'{}\'", t);
                        }
                    });
                });
                sleepInterrupted(timeConfig.scanIntervalMs(), MILLISECONDS);
            }
        } catch (Exception e) {
            logger.error("Error in time of processing", e);
        } finally {
            turnOffWatcher();
            logger.info("Second level watcher stopped");
            awaitStop.countDown();
        }
    }

    public void stop() throws InterruptedException {
        logger.info("Second level watcher stopping...");
        turnOffWatcher();
        awaitStop.await(2, SECONDS);
    }

    private BlockingQueue<TimeTask> nextQueue() {
        int min = Integer.MAX_VALUE;
        int index = 0;
        for (int i = 0, queueSecondLevelSize = queueSecondLevel.size(); i < queueSecondLevelSize; i++) {
            if (min > queueSecondLevel.get(i).size()) {
                min = queueSecondLevel.get(i).size();
                index = i;
            }
        }
        return queueSecondLevel.get(index);
    }

    private Map<Long, List<TimeTask>> getHotSecondLevelTasks() {
        final long checkTime = hzObjects.getClusterTime() + timeConfig.hotTaskUpperBoundMs();
        final Set<Long> localHotKeys = new HashSet<>(mapSecondLevel
                .localKeySet(Predicates.lessEqual("scheduledTime", checkTime)));
//                .localKeySet((Predicate<Long, TimeTask>) e -> e.getValue().getScheduledTime() - now <= upperBound));
        return (localHotKeys.isEmpty()
                ? Collections.<TimeTask>emptyList()
                : mapSecondLevel.values((Predicate<Long, TimeTask>) e -> localHotKeys.contains(e.getKey())))
                .stream().collect(Collectors.groupingBy(TimeTask::getScheduledTime, Collectors.toList()));
    }

    private void turnOffWatcher() {
        SL_WATCHER_STATE.compareAndSet(true, false);
    }
}


















