package ru.shishmakov.core;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.concurrent.LifeCycle;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
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
    private static final String NODE_SYSTEM_KEY = "node";
    private static final AtomicReference<LifeCycle> NODE_STATE = new AtomicReference<>(IDLE);
    private static final AtomicReference<LifeCycle> SERVICES_STATE = new AtomicReference<>(IDLE);
    private static final CountDownLatch awaitStart = new CountDownLatch(1);

    @Inject
    @Named("node.executor")
    private ExecutorService executor;
    @Inject
    private HazelcastService hzService;
    @Nullable
    private volatile ServiceManager sm;
    private final int nodeNumber;

    public Node() {
        this.nodeNumber = Integer.valueOf(System.getProperty(NODE_SYSTEM_KEY, "0"));
    }

    @PostConstruct
    public void setUp() {
        logger.info("----- // -----    NODE: {} START {}    ----- // -----", nodeNumber, LocalDateTime.now());
    }

    @PreDestroy
    public void tearDown() {
        logger.info("----- // -----    NODE: {} STOP {}    ----- // -----", nodeNumber, LocalDateTime.now());
    }


    public Node startAsync() {
        new Thread(this::start).start();
        return this;
    }

    public Node start() {
        logger.info("Node: {} starting...", nodeNumber);

        final LifeCycle state = NODE_STATE.get();
        if (LifeCycle.isNotIdle(state)) {
            logger.warn("Warning! Node: {} already started, state: {}", nodeNumber, state);
            return this;
        }
        NODE_STATE.set(INIT);
        awaitStart.countDown();
        startServices(hzService);
        assignThreadHook();

        NODE_STATE.set(RUN);
        logger.info("Node: {} started, state: {}", nodeNumber, NODE_STATE.get());
        return this;
    }

    public void stop() {
        logger.info("Node: {} stopping...", nodeNumber);
        final LifeCycle state = NODE_STATE.get();
        if (LifeCycle.isNotRun(state)) {
            logger.warn("Warning! Node: {} already stopped, state: {}", nodeNumber, state);
            return;
        }

        try {
            NODE_STATE.set(STOPPING);
            stopServices();
            stopExecutors();
        } finally {
            NODE_STATE.set(IDLE);
            logger.info("Node: {} stopped, state: {}", nodeNumber, NODE_STATE.get());
        }
    }

    public void await() throws InterruptedException {
        awaitStart.await();
        logger.info("Node: {} thread: {} await the state: {} to stop itself", nodeNumber, Thread.currentThread(), IDLE);
        for (long count = 0; LifeCycle.isNotIdle(NODE_STATE.get()); count++) {
            if (count % 100 == 0) logger.debug("Thread: {} is alive", Thread.currentThread());
            sleepWithoutInterrupted(100, MILLISECONDS);
        }
    }

    private void startServices(Service service, Service... services) {
        logger.info("Node: {} services starting...", nodeNumber);
        final LifeCycle state = SERVICES_STATE.get();
        if (LifeCycle.isNotIdle(state)) {
            logger.warn("Warning! Node: {} services already started, state: {}", nodeNumber, state);
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
        logger.info("Node: {} services started, state: {}", nodeNumber, SERVICES_STATE.get());
    }

    private void stopServices() {
        logger.info("Node: {} services stopping...", nodeNumber);
        final LifeCycle state = SERVICES_STATE.get();
        if (LifeCycle.isNotRun(state)) {
            logger.warn("Warning! Node: {} services already stopped, state: {}", nodeNumber, state);
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
            logger.info("Node: {} services stopped, state: {}", nodeNumber, SERVICES_STATE.get());
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
