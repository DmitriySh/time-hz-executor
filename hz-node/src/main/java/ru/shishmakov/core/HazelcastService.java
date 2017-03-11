package ru.shishmakov.core;

import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Dmitriy Shishmakov on 11.03.17
 */
@Singleton
public class HazelcastService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(HazelcastService.class);


    @Override
    protected void doStart() {
        logger.info("Hz service starting...");
        try {

            notifyStarted();
            logger.info("Hz service started");
        } catch (Throwable e) {
            notifyFailed(e);
        }
    }

    @Override
    protected void doStop() {
        logger.info("Hz service stopping...");
        try {

            notifyStopped();
            logger.info("Hz service stopped");
        } catch (Throwable e) {
            notifyFailed(e);
        }
    }
}
