package ru.shishmakov.core;

import com.hazelcast.core.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.config.TimeConfig;
import ru.shishmakov.hz.HzObjects;
import ru.shishmakov.hz.TimeTask;
import ru.shishmakov.util.QueueUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.shishmakov.concurrent.LifeCycle.RUN;
import static ru.shishmakov.concurrent.Threads.sleepInterrupted;

/**
 * @author Dmitriy Shishmakov on 18.03.17
 */
@Singleton
public abstract class LevelConsumer {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Inject
    private TimeConfig timeConfig;
    @Inject
    protected HzObjects hzObjects;

    private final String name = this.getClass().getSimpleName();
    private final AtomicBoolean consumerState = new AtomicBoolean(true);
    private final CountDownLatch awaitStop = new CountDownLatch(1);
    protected int selfNumber;
    private int ownerNumber;
    private String ownerName;

    public void setMetaInfo(int ownerNumber, String ownerName) {
        this.ownerNumber = ownerNumber;
        this.ownerName = ownerName;
    }

    public void start() {
        logger.info("{}:{}  {}:{} started", name, selfNumber, ownerName, ownerNumber);
        try {
            while (consumerState.get() && !Thread.currentThread().isInterrupted()) {
                QueueUtils.poll(getQueue()).ifPresent(t -> {
                    t.setState(RUN);
                    logger.debug("<--  {}:{}  {}:{} start process task \'{}\' ...", name, selfNumber, ownerName, ownerNumber, t);
                    try {
                        t.call();
                    } catch (Exception e) {
                        logger.error("X--X  {}:{}  {}:{} failed process task '{}'", name, selfNumber, ownerName, ownerNumber, e);
                    } finally {
                        getIMap().delete(t.getOrderId());
                    }
                });
                sleepInterrupted(timeConfig.scanIntervalMs(), MILLISECONDS);
            }
        } catch (Exception e) {
            logger.error("{}:{}  {}:{} error in time of processing", name, selfNumber, ownerName, ownerNumber, e);
        } finally {
            shutdownWatcher();
            awaitStop.countDown();
        }
    }

    public void stop() {
        logger.info("{}:{}  {}:{} stopping...", name, selfNumber, ownerName, ownerNumber);
        try {
            shutdownWatcher();
            awaitStop.await(2, SECONDS);
            logger.info("{}:{}  {}:{} stopped", name, selfNumber, ownerName, ownerNumber);
        } catch (Exception e) {
            logger.error("{}:{}  {}:{} error in time of stopping", name, selfNumber, ownerName, ownerNumber, e);
        }
    }

    protected abstract BlockingQueue<TimeTask> getQueue();

    protected abstract IMap<Long, TimeTask> getIMap();

    private void shutdownWatcher() {
        if (consumerState.compareAndSet(true, false)) {
            logger.debug("{}:{}  {}:{} waiting for shutdown process to complete...", name, selfNumber, ownerName, ownerNumber);
        }
    }
}
