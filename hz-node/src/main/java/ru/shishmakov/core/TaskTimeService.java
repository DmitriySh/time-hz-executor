package ru.shishmakov.core;

import com.google.common.util.concurrent.AbstractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.config.HzConfig;
import ru.shishmakov.hz.HzService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

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
    private HzConfig hzConfig;
    @Inject
    private HzService hzService;
    @Inject
    private FirstLevelWatcher flWatcher;
    @Inject
    private FirstLevelConsumer flConsumer;
    @Inject
    private SecondLevelWatcher slWatcher;
    @Inject
    private SecondLevelConsumer slConsumer;

    @Override
    protected void doStart() {
        logger.info("Task time service {} starting...");
        try {
            startTimeService();
            notifyStarted();
            logger.info("Task time service {} started");
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
            logger.info("Task time service {} stopped");
        } catch (Throwable e) {
            notifyFailed(e);
        }
    }

    protected void startTimeService() throws TimeoutException {
        hzService.awaitRunning(hzConfig.clientInitialWaitTimeoutSec(), SECONDS);
        executor.execute(() -> flWatcher.start());
        executor.execute(() -> slWatcher.start());
    }

//    private FileParser[] runParserTasks(int count) {
//        final FileParser[] parsers = new FileParser[count];
//        for (int i = 0; i < count; i++) {
//            final FileParser fileParser = getFileParser();
//            parsers[i] = fileParser;
//            executor.execute(fileParser::start);
//        }
//        return parsers;
//    }


    protected void stopTimeService() throws InterruptedException {
        flWatcher.stop();
        slWatcher.stop();
    }
}
