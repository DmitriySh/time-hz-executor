package ru.shishmakov.core;

import com.google.common.util.concurrent.AbstractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.config.HzConfig;
import ru.shishmakov.hz.HzDOController;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;

/**
 * @author Dmitriy Shishmakov on 15.03.17
 */
@Singleton
public class TaskTimeService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    @Named("node.executor")
    private ExecutorService executor;
    @Inject
    private TaskFirstLevelController flController;
    @Inject
    private TaskSecondLevelController slController;
    @Inject
    private HzDOController distObjects;
    @Inject
    private HzConfig hzConfig;

    @Override
    protected void doStart() {
        logger.info("Task time service {} starting...");
        try {
            startTimeService();
            notifyStarted();
            logger.info("ask time service {} started");
        } catch (Throwable e) {
            notifyFailed(e);
        }
    }

    @Override
    protected void doStop() {
        logger.info("Task time service {} stopping...");
        try {
            stopTimeService();
            notifyStopped();
            logger.info("ask time service {} started");
        } catch (Throwable e) {
            notifyFailed(e);
        }
    }

    protected void startTimeService() {
        executor.execute(() -> flController.start());
        executor.execute(() -> slController.start());
    }

    protected void stopTimeService() {

    }
}
