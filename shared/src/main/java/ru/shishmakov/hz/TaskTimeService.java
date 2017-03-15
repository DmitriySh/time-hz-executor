package ru.shishmakov.hz;

import com.google.common.util.concurrent.AbstractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author <a href="mailto:d.shishmakov@corp.nekki.ru">Shishmakov Dmitriy</a>
 */
@Singleton
public class TaskTimeService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ScheduledExecutorService executorService;

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
