package ru.shishmakov.core;

import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.config.TimeConfig;
import ru.shishmakov.hz.HzObjects;
import ru.shishmakov.hz.HzService;
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
    private static final String NAME = MethodHandles.lookup().lookupClass().getSimpleName();

    private static final AtomicBoolean FL_WATCHER_STATE = new AtomicBoolean(true);

    @Inject
    private TimeConfig timeConfig;
    @Inject
    private HzService hzService;
    @Inject
    private HzObjects hzObjects;
    @Inject
    @Named("timeQueue.firstLevel")
    public BlockingQueue<TimeTask> queueFirstLevel;

    private final CountDownLatch awaitStop = new CountDownLatch(1);
    private IMap<Long, TimeTask> mapFirstLevel;
    private int ownerNumber;
    private String ownerName;

    public void setMetaInfo(int ownerNumber, String ownerName) {
        this.ownerNumber = ownerNumber;
        this.ownerName = ownerName;
    }

    public void start() {
        this.mapFirstLevel = hzObjects.getFirstLevelMap();
        try {
            logger.info("{} {}:{} started", NAME, ownerName, ownerNumber);
            while (FL_WATCHER_STATE.get() && !Thread.currentThread().isInterrupted()) {
                if (hzService.hasHzInstance()) process();
                else logger.warn("{} {}:{} hz instance is not available!", NAME, ownerName, ownerNumber);

                sleepInterrupted(timeConfig.scanIntervalMs(), MILLISECONDS);
            }
        } catch (Exception e) {
            logger.error("{} {}:{} error in time of processing", NAME, ownerName, ownerNumber, e);
        } finally {
            shutdownWatcher();
            awaitStop.countDown();
        }
    }

    private void process() {
        final long now = hzObjects.getClusterTime();
        getHotFirstLevelTasks().forEach(t -> {
            logger.debug("<--  {} {}:{} take task \'{}\'; now: {}, scheduledTime: {}, delta: {}",
                    NAME, ownerName, ownerNumber, t, now, t.getScheduledTime(), t.getScheduledTime() - now);
            if (QueueUtils.offer(queueFirstLevel, t)) {
                mapFirstLevel.removeAsync(t.getOrderId());
                logger.debug("-->  {} {}:{} put task \'{}\'", NAME, ownerName, ownerNumber, t);
            }
        });
    }

    public void stop() throws InterruptedException {
        logger.info("{} {}:{} stopping...", NAME, ownerName, ownerNumber);
        shutdownWatcher();
        awaitStop.await(2, SECONDS);
        logger.info("{} {}:{} stopped", NAME, ownerName, ownerNumber);
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

    private void shutdownWatcher() {
        if (FL_WATCHER_STATE.compareAndSet(true, false)) {
            logger.debug("{} {}:{} waiting for shutdown process to complete...", NAME, ownerName, ownerNumber);
        }
    }
}
