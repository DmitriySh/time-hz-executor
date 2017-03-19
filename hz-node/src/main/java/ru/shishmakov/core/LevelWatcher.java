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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.shishmakov.concurrent.LifeCycle.IDLE;
import static ru.shishmakov.concurrent.LifeCycle.INIT;
import static ru.shishmakov.concurrent.Threads.sleepInterrupted;

/**
 * @author Dmitriy Shishmakov on 16.03.17
 */
public abstract class LevelWatcher {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String NAME = this.getClass().getSimpleName();

    @Inject
    private TimeConfig timeConfig;
    @Inject
    private HzService hzService;
    @Inject
    protected HzObjects hzObjects;

    private final AtomicBoolean watcherState = new AtomicBoolean(true);
    private final CountDownLatch awaitStop = new CountDownLatch(1);
    private int ownerNumber;
    private String ownerName;

    public void setMetaInfo(int ownerNumber, String ownerName) {
        this.ownerNumber = ownerNumber;
        this.ownerName = ownerName;
    }

    public void start() {
        logger.info("{} {}:{} started", NAME, ownerName, ownerNumber);
        try {
            while (watcherState.get() && !Thread.currentThread().isInterrupted()) {
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

    public void stop() throws InterruptedException {
        logger.info("{} {}:{} stopping...", NAME, ownerName, ownerNumber);
        shutdownWatcher();
        awaitStop.await(2, SECONDS);
        logger.info("{} {}:{} stopped", NAME, ownerName, ownerNumber);
    }

    protected abstract BlockingQueue<TimeTask> getQueue();

    protected abstract IMap<Long, TimeTask> getIMap();

    private void process() {
        final long now = hzObjects.getClusterTime();
        getHotLevelTasks(now).forEach(t -> {
            logger.debug("<--  {} {}:{} take task \'{}\'; now: {}, scheduledTime: {}, delta: {}",
                    NAME, ownerName, ownerNumber, t, now, t.getScheduledTime(), t.getScheduledTime() - now);
            if (QueueUtils.offer(getQueue(), t)) {
                t.setState(INIT);
                logger.debug("-->  {} {}:{} put task \'{}\'", NAME, ownerName, ownerNumber, t);
            }
        });
    }

    private Collection<TimeTask> getHotLevelTasks(long now) {
        final Set<Long> localHotKeys = new HashSet<>(getIMap()
                .localKeySet(Predicates.and(Predicates.lessEqual("scheduledTime", now), Predicates.equal("state", IDLE))));
//                .localKeySet((Predicate<Long, TimeTask>) e -> e.getValue().getScheduledTime() - now <= upperBound));
        return localHotKeys.isEmpty()
                ? Collections.emptyList()
                : getIMap().values((Predicate<Long, TimeTask>) e -> localHotKeys.contains(e.getKey()));
    }

    private void shutdownWatcher() {
        if (watcherState.compareAndSet(true, false)) {
            logger.debug("{} {}:{} waiting for shutdown process to complete...", NAME, ownerName, ownerNumber);
        }
    }
}
