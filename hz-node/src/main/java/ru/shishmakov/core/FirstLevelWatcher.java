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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.shishmakov.concurrent.Threads.sleepInterrupted;

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
    private final CountDownLatch awaitStop = new CountDownLatch(1);

    public void start() {
        this.mapFirstLevel = hzObjects.getFirstLevelMap();
        try {
            logger.info("First level watcher started");
            while (FL_WATCHER_STATE.get() && !Thread.currentThread().isInterrupted()) {
                final long now = hzObjects.getClusterTime();
                getHotFirstLevelTasks().forEach(t -> {
                    logger.debug("<--  take fL task \'{}\'; now: {}, scheduledTime: {}, delta: {}",
                            t, now, t.getScheduledTime(), t.getScheduledTime() - now);
                    if (QueueUtils.offer(queueFirstLevel, t)) {
                        mapFirstLevel.removeAsync(t.getOrderId());
                        logger.debug("-->  put FL task \'{}\' : firstLevel PriorityBlockingQueue", t);
                    }
                });
                sleepInterrupted(timeConfig.scanIntervalMs(), MILLISECONDS);
            }
        } catch (Exception e) {
            logger.error("Error in time of processing", e);
        } finally {
            turnOffWatcher();
            logger.info("First level watcher stopped");
            awaitStop.countDown();
        }
    }

    public void stop() throws InterruptedException {
        logger.info("First level watcher stopping...");
        turnOffWatcher();
        awaitStop.await(2, SECONDS);
    }

    private Collection<TimeTask> getHotFirstLevelTasks() {
        final long checkTime = hzObjects.getClusterTime() + timeConfig.hotTaskUpperBoundMs();
        final Set<Long> localHotKeys = new HashSet<>(mapFirstLevel
                .localKeySet(Predicates.lessEqual("scheduledTime", checkTime)));
//                .localKeySet((Predicate<Long, TimeTask>) e -> e.getValue().getScheduledTime() - now <= upperBound));
        return localHotKeys.isEmpty()
                ? Collections.emptyList()
                : mapFirstLevel.values((Predicate<Long, TimeTask>) e -> localHotKeys.contains(e.getKey()));
    }

    private void turnOffWatcher() {
        FL_WATCHER_STATE.compareAndSet(true, false);
    }
}
