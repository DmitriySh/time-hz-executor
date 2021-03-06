package ru.shishmakov.core;

import com.google.common.util.concurrent.AbstractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.config.HzConfig;
import ru.shishmakov.hz.HzService;
import ru.shishmakov.hz.TimeTask;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Service declares the rules to start and shutdown watchers and consumers for {@link TimeTask}
 *
 * @author Dmitriy Shishmakov on 15.03.17
 */
@Singleton
public class TaskTimeService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String NAME = MethodHandles.lookup().lookupClass().getSimpleName();

    @Inject
    @Named("node.executor")
    private ExecutorService executor;
    @Inject
    private HzConfig hzConfig;
    @Inject
    private HzService hzService;
    @Inject
    private FirstLevelWatcher flWatcher;
    @Inject
    private SecondLevelWatcher slWatcher;
    private Provider<FirstLevelConsumer> flConsumer;
    private Provider<SecondLevelConsumer> slConsumer;
    private final List<LevelConsumer> consumers = new ArrayList<>();
    private int ownerNumber;
    private String ownerName;

    @Inject
    public TaskTimeService(Provider<FirstLevelConsumer> flConsumer, Provider<SecondLevelConsumer> slConsumer) {
        this.flConsumer = flConsumer;
        this.slConsumer = slConsumer;
    }

    public void setMetaInfo(int ownerNumber, String ownerName) {
        this.ownerNumber = ownerNumber;
        this.ownerName = ownerName;
    }

    @Override
    protected void doStart() {
        logger.info("{} {}:{} starting...", NAME, ownerName, ownerNumber);
        try {
            startTimeService();
            notifyStarted();
            logger.info("{} {}:{} started", NAME, ownerName, ownerNumber);
        } catch (Throwable e) {
            notifyFailed(e);
        }
    }

    @Override
    protected void doStop() {
        logger.info("{} {}:{} stopping...", NAME, ownerName, ownerNumber);
        try {
            stopTimeService();
            notifyStopped();
            logger.info("{} {}:{} stopped", NAME, ownerName, ownerNumber);
        } catch (Throwable e) {
            notifyFailed(e);
        }
    }

    protected void startTimeService() throws TimeoutException {
        hzService.awaitRunning(hzConfig.clientInitialWaitTimeoutSec(), SECONDS);
        flWatcher.setMetaInfo(ownerNumber, ownerName);
        slWatcher.setMetaInfo(ownerNumber, ownerName);
        executor.execute(() -> flWatcher.start());
        executor.execute(() -> slWatcher.start());

        for (int count = Math.max(1, Runtime.getRuntime().availableProcessors() / 2); count > 0; count--) {
            executor.execute(() -> defineLevelConsumer(flConsumer.get()).start());
            executor.execute(() -> defineLevelConsumer(slConsumer.get()).start());
        }
    }

    private LevelConsumer defineLevelConsumer(LevelConsumer consumer) {
        consumer.setMetaInfo(ownerNumber, ownerName);
        consumers.add(consumer);
        return consumer;
    }

    protected void stopTimeService() throws InterruptedException {
        flWatcher.stop();
        slWatcher.stop();
        consumers.forEach(LevelConsumer::stop);
    }
}
