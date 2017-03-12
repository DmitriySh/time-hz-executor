package ru.shishmakov.core;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.concurrent.LifeCycle;
import ru.shishmakov.config.TimeConfig;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Thread.MAX_PRIORITY;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.shishmakov.concurrent.LifeCycle.*;

/**
 * @author Dmitriy Shishmakov on 10.03.17
 */
public class Node {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long STOP_TIMEOUT = 10;
    private static final AtomicReference<LifeCycle> NODE_STATE = new AtomicReference<>(IDLE);
    private static final AtomicReference<LifeCycle> SERVICES_STATE = new AtomicReference<>(IDLE);

    @Inject
    private TimeConfig timeConfig;
    @Inject
    @Named("node.executor")
    private ExecutorService executor;
    @Inject
    private HazelcastService hzService;
    @Nullable
    private volatile ServiceManager sm;

    public Node startAsync() {
        new Thread(this::start).start();
        return this;
    }

    public Node start() {
        logger.info("Node starting...");

        final LifeCycle state = NODE_STATE.get();
        if (LifeCycle.isNotIdle(state)) {
            logger.warn("Warning! Node already started, state: {}", state);
            return this;
        }
        NODE_STATE.set(INIT);
        startServices(hzService);
        assignThreadHook();

        NODE_STATE.set(RUN);
        logger.info("Node started, state: {}", NODE_STATE.get());
        return this;
    }

    public void stop() {
        logger.info("Node stopping...");
        final LifeCycle state = NODE_STATE.get();
        if (LifeCycle.isNotRun(state)) {
            logger.warn("Warning! Node already stopped, state: {}", state);
            return;
        }

        try {
            NODE_STATE.set(STOPPING);
            stopServices();
            stopExecutors();
        } finally {
            NODE_STATE.set(IDLE);
            logger.info("Node stopped, state: {}", NODE_STATE.get());
        }
    }

    public void await() {
        logger.info("Node thread: {} await the state: {} to stop itself", Thread.currentThread(), IDLE);
        for (long count = 0; LifeCycle.isNotIdle(NODE_STATE.get()); count++) {
            if (count % 100 == 0) logger.debug("Thread: {} is alive", Thread.currentThread());
            sleepWithoutInterrupted(100, MILLISECONDS);
        }
    }

    private void startServices(Service service, Service... services) {
        logger.info("Node services starting...");
        final LifeCycle state = SERVICES_STATE.get();
        if (LifeCycle.isNotIdle(state)) {
            logger.warn("Warning! Node services already started, state: {}", state);
            return;
        }

        SERVICES_STATE.set(INIT);
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
        SERVICES_STATE.set(RUN);
        logger.info("Node services started, state: {}", SERVICES_STATE.get());
    }

    private void stopServices() {
        logger.info("Node services stopping...");
        final LifeCycle state = SERVICES_STATE.get();
        if (LifeCycle.isNotRun(state)) {
            logger.warn("Warning! Node services already stopped, state: {}", state);
            return;
        }

        try {
            SERVICES_STATE.set(STOPPING);
            checkNotNull(sm, "Service manager is null").stopAsync().awaitStopped(STOP_TIMEOUT, SECONDS);
            sm = null;
        } catch (Throwable e) {
            logger.error("Exception occurred during stopping node services", e);
        } finally {
            SERVICES_STATE.set(IDLE);
            logger.info("Node services stopped, state: {}", SERVICES_STATE.get());
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

    private void assignThreadHook() {
        final Thread hook = new Thread(() -> {
            logger.debug("Thread: {} was interrupted by hook", Thread.currentThread());
            stop();
        });
        hook.setName("node-hook-thread");
        hook.setPriority(MAX_PRIORITY);
        Runtime.getRuntime().addShutdownHook(hook);
    }


    private static void sleepWithoutInterrupted(long timeout, TimeUnit unit) {
        try {
            Thread.sleep(unit.toMillis(timeout));
        } catch (InterruptedException ignored) {
        }
    }
}
