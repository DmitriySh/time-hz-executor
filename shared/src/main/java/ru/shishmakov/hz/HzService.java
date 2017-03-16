package ru.shishmakov.hz;

import com.google.common.util.concurrent.AbstractService;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.config.HzConfig;

import javax.annotation.PostConstruct;
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
    private String label;

    @PostConstruct
    public void setUp() {
        this.label = hzConfig.server() ? "server" : "client";
    }

    public HazelcastInstance getHzInstance() {
        return HZ_INSTANCE.get();
    }

    @Override
    protected void doStart() {
        logger.info("Hz {} service starting...", label);
        try {
            startHz();
            notifyStarted();
            logger.info("Hz {} service started", label);
        } catch (Throwable e) {
            notifyFailed(e);
        }
    }

    @Override
    protected void doStop() {
        logger.info("Hz {} service stopping...", label);
        try {
            stopHz();
            notifyStopped();
            logger.info("Hz {} service stopped", label);
        } catch (Throwable e) {
            notifyFailed(e);
        }
    }

    protected void startHz() throws InterruptedException {
        final HazelcastInstance current = HzBuilder.instance(hzConfig).build();
        if (HZ_INSTANCE.getAndSet(current) != null) {
            logger.warn("Warning! Hz {} service already has instance", label);
        }
    }

    protected void stopHz() {
        final HazelcastInstance current = HZ_INSTANCE.getAndSet(null);
        if (current != null) current.shutdown();
        else logger.warn("Warning! Hz {} service is not available to stop", label);
    }
}
