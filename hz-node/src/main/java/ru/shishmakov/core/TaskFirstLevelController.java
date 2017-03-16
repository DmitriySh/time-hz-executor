package ru.shishmakov.core;

import com.hazelcast.core.IMap;
import com.hazelcast.query.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.concurrent.Threads;
import ru.shishmakov.config.TimeConfig;
import ru.shishmakov.hz.HzDOController;
import ru.shishmakov.hz.TaskTime;
import ru.shishmakov.util.QueueUtils;

import javax.annotation.PostConstruct;
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
public class TaskFirstLevelController {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private TimeConfig timeConfig;
    @Inject
    private HzDOController doService;
    @Inject
    @Named("timeQueue.firstLevel")
    public BlockingQueue<TaskTime> queueFirstLevel;

    private IMap<Long, TaskTime> firstLevelMap;
    private final AtomicBoolean FL_STATE = new AtomicBoolean(true);

    @PostConstruct
    public void setUp() {
        this.firstLevelMap = doService.getFirstLevelMap();
    }

    public void start() {
        logger.info("Initialise file persist {} ...");
        try {
            while (FL_STATE.get()) {
                for (TaskTime task : getHotFirstLevelTasks()) {
                    logger.debug("<--  take FL task \'{}\' : firstLevel IMap", task);

                    QueueUtils.offer(queueFirstLevel, task);
                    logger.debug("-->  put FL task \'{}\' : firstLevel PriorityBlockingQueue", task);

                    Threads.sleepWithInterrupted(timeConfig.scanIntervalMs(), MILLISECONDS);
                }
            }
        } catch (Exception e) {
            logger.error("Error in time of processing", e);
        } finally {
            FL_STATE.compareAndSet(true, false);
        }
    }

    public void stop() {
        logger.info("Finalization persist {} ...");
        FL_STATE.compareAndSet(true, false);
    }

    private Collection<TaskTime> getHotFirstLevelTasks() {
        final long upperBound = timeConfig.firstLevelUpperBound();
        final Set<Long> localHotKeys = new HashSet<>(firstLevelMap
                .localKeySet((Predicate<Long, TaskTime>) e -> e.getValue().getScheduledTime() <= upperBound));
        return localHotKeys.isEmpty()
                ? Collections.emptyList()
                : firstLevelMap.values((Predicate<Long, TaskTime>) e -> localHotKeys.contains(e.getKey()));
    }
}
