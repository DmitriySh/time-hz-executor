package ru.shishmakov.core;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.config.TimeConfig;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author Dmitriy Shishmakov on 10.03.17
 */
public class Node {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long STOP_TIMEOUT = 10;
    private static final AtomicBoolean nodeUp = new AtomicBoolean(false);
    private static final AtomicBoolean servicesUp = new AtomicBoolean(false);

    @Inject
    private TimeConfig timeConfig;
    @Inject
    @Named("node.executor")
    private ExecutorService executor;
    @Inject
    private HazelcastService hzService;

    private ServiceManager sm;

    public void start() {
        logger.info("Node starting...");

        if (nodeUp.getAndSet(true)) {
            logger.warn("Warning! Node already started!");
            return;
        }
        startService(hzService);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        logger.info("Node started");
    }

    public void stop() {
        logger.info("Node stopping...");
        if (!nodeUp.getAndSet(false)) {
            logger.warn("Warning! Node already stopped!");
            return;
        }

        stopServices();
        stopExecutors();
        logger.info("Node stopped");
    }

    private void startService(Service service, Service... services) {
        logger.info("Node services starting...");
        if (servicesUp.getAndSet(true)) {
            logger.warn("Warning! Node services already started!");
            return;
        }

        final ServiceManager sm = new ServiceManager(Lists.asList(service, services));
        sm.addListener(new ServiceManager.Listener() {
            @Override
            public void healthy() {
                logger.info("Listener: node has started all services  -->");
            }

            @Override
            public void stopped() {
                logger.info("Listener: node has stopped all services  <--");
            }

            @Override
            public void failure(Service service) {
                logger.error("Error! All the node services has crashed: {}  X--X", service, service.failureCause());
            }
        }, MoreExecutors.directExecutor());
        sm.startAsync().awaitHealthy();
        this.sm = sm;
        logger.info("Node services started");
    }

    private void stopServices() {
        logger.info("Node services stopping...");
        if (!servicesUp.getAndSet(false)) {
            logger.warn("Warning! Node services already stopped!");
            return;
        }
        if (sm != null) {
            try {
                sm.stopAsync().awaitStopped(STOP_TIMEOUT, SECONDS);
                sm = null;
                logger.info("Node services stopped");
            } catch (Throwable e) {
                logger.error("Exception occurred during stopping node services", e);
            }
        }
    }

    private void stopExecutors() {
        logger.info("Executor services stopping...");
        try {
            MoreExecutors.shutdownAndAwaitTermination(executor, STOP_TIMEOUT, SECONDS);
            logger.info("Executor services stopped");
        } catch (Throwable e) {
            logger.error("Exception occurred during stopping executor services", e);
        }
    }
}
