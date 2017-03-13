package ru.shishmakov.hz;

import com.google.common.util.concurrent.AbstractService;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.config.HzConfig;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Dmitriy Shishmakov on 11.03.17
 */
@Singleton
public class HzService extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final AtomicReference<HazelcastInstance> HZ_INSTANCE = new AtomicReference<>();

    @Inject
    private HzConfig hzConfig;

    @Override
    protected void doStart() {
        logger.info("Hz service starting...");
        try {
            startHzNode();
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
            stopHzNode();
            notifyStopped();
            logger.info("Hz service stopped");
        } catch (Throwable e) {
            notifyFailed(e);
        }
    }

    protected void startHzNode() {
        final HazelcastInstance current = HzBuilder.instance(hzConfig.server()).build();
        if (HZ_INSTANCE.getAndSet(current) != null) {
            logger.warn("Warning! Hz service already has instance");
        }
    }

    protected void stopHzNode() {
        final HazelcastInstance current = HZ_INSTANCE.getAndSet(null);
        if (current != null) current.shutdown();
        else logger.warn("Warning! Hz service is not available to stop");
    }
}
