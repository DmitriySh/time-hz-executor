package ru.shishmakov.hz;

import com.google.common.util.concurrent.AbstractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.config.HzConfig;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author <a href="mailto:d.shishmakov@corp.nekki.ru">Shishmakov Dmitriy</a>
 */
@Singleton
public class TaskTimeService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ScheduledExecutorService executorService;
    @Inject
    private HzObjects distObjects;
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

    }

    protected void stopTimeService() {

    }
}
