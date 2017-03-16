package ru.shishmakov.core;

import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.concurrent.LifeCycle;
import ru.shishmakov.concurrent.ServiceController;
import ru.shishmakov.hz.HzService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.shishmakov.concurrent.LifeCycle.*;
import static ru.shishmakov.concurrent.Threads.*;

/**
 * @author Dmitriy Shishmakov on 10.03.17
 */
public class Node {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String NODE_SYSTEM_KEY = "node";
    private static final AtomicReference<LifeCycle> NODE_STATE = new AtomicReference<>(IDLE);
    private static final CountDownLatch awaitStart = new CountDownLatch(1);

    @Inject
    @Named("node.executor")
    private ExecutorService executor;
    @Inject
    private HzService hzService;
    @Inject
    private TaskTimeService timeService;
    private final ServiceController serviceController;

    private final int nodeNumber;

    public Node() {
        this.nodeNumber = Integer.valueOf(System.getProperty(NODE_SYSTEM_KEY, "0"));
        this.serviceController = new ServiceController(nodeNumber, "Node");
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
        new Thread(this::start, "node-hz-" + nodeNumber).start();
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
        serviceController.startServices(hzService, timeService);
        assignThreadHook(this::stop, "node-" + nodeNumber + "-hook-thread");

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
            serviceController.stopServices();
            stopExecutors();
        } finally {
            NODE_STATE.set(IDLE);
            logger.info("Node: {} stopped, state: {}", nodeNumber, NODE_STATE.get());
        }
    }

    public void await() throws InterruptedException {
        awaitStart.await();
        Thread.currentThread().setName("node-main-" + nodeNumber);
        logger.info("Node: {} thread: {} await the state: {} to stop itself", nodeNumber, Thread.currentThread(), IDLE);
        for (long count = 0; LifeCycle.isNotIdle(NODE_STATE.get()); count++) {
            if (count % 100 == 0) logger.debug("Thread: {} is alive", Thread.currentThread());
            sleepWithoutInterrupted(100, MILLISECONDS);
        }
    }

    private void stopExecutors() {
        logger.info("Executor services stopping...");
        try {
            MoreExecutors.shutdownAndAwaitTermination(executor, STOP_TIMEOUT_SEC, SECONDS);
            logger.info("Executor services stopped");
        } catch (Throwable e) {
            logger.error("Exception occurred during stopping executor services", e);
        }
    }
}
