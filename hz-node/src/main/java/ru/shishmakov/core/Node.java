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
import javax.inject.Singleton;
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
@Singleton
public class Node {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String NODE_SYSTEM_KEY = "node";
    private static final String NAME = MethodHandles.lookup().lookupClass().getSimpleName();
    private static final AtomicReference<LifeCycle> NODE_STATE = new AtomicReference<>(IDLE);
    private static final CountDownLatch awaitStart = new CountDownLatch(1);

    @Inject
    @Named("node.executor")
    private ExecutorService executor;
    @Inject
    private HzService hzService;
    @Inject
    private TaskTimeService timeService;
    @Inject
    private ServiceController serviceController;
    private final int nodeNumber;

    public Node() {
        this.nodeNumber = Integer.valueOf(System.getProperty(NODE_SYSTEM_KEY, "0"));
    }

    @PostConstruct
    public void setUp() {
        logger.info("----- // -----    {}: {} START {}    ----- // -----", NAME, nodeNumber, LocalDateTime.now());
        this.timeService.setMetaInfo(nodeNumber, "Node");
        this.serviceController.setMetaInfo(nodeNumber, NAME);
    }

    @PreDestroy
    public void tearDown() {
        logger.info("----- // -----    {}: {} STOP {}    ----- // -----", NAME, nodeNumber, LocalDateTime.now());
    }

    public Node startAsync() {
        new Thread(this::start, "node-hz-" + nodeNumber).start();
        return this;
    }

    public Node start() {
        logger.info("{}: {} starting...", NAME, nodeNumber);

        final LifeCycle state = NODE_STATE.get();
        if (LifeCycle.isNotIdle(state)) {
            logger.warn("Warning! {}: {} already started, state: {}", NAME, nodeNumber, state);
            return this;
        }
        NODE_STATE.set(INIT);
        awaitStart.countDown();
        serviceController.startServices(hzService, timeService);
        assignThreadHook(this::stop, "node-" + nodeNumber + "-hook-thread");

        NODE_STATE.set(RUN);
        logger.info("{}: {} started, state: {}", NAME, nodeNumber, NODE_STATE.get());
        return this;
    }


    public void stop() {
        logger.info("{}: {} stopping...", NAME, nodeNumber);
        final LifeCycle state = NODE_STATE.get();
        if (LifeCycle.isNotRun(state)) {
            logger.warn("Warning! {}: {} already stopped, state: {}", NAME, nodeNumber, state);
            return;
        }

        try {
            NODE_STATE.set(STOPPING);
            serviceController.stopServices();
            stopExecutors();
        } finally {
            NODE_STATE.set(IDLE);
            logger.info("{}: {} stopped, state: {}", NAME, nodeNumber, NODE_STATE.get());
        }
    }

    public void await() throws InterruptedException {
        awaitStart.await();
        Thread.currentThread().setName("node-main-" + nodeNumber);
        logger.info("{}: {} thread: {} await the state: {} to stop itself", NAME, nodeNumber, Thread.currentThread(), IDLE);
        for (long count = 0; LifeCycle.isNotIdle(NODE_STATE.get()); count++) {
            if (count % 100 == 0) logger.debug("Thread: {} is alive", Thread.currentThread());
            sleepWithoutInterruptedAfterTimeout(100, MILLISECONDS);
        }
    }

    private void stopExecutors() {
        logger.info("{}: {} executor services stopping...", NAME, nodeNumber);
        try {
            MoreExecutors.shutdownAndAwaitTermination(executor, STOP_TIMEOUT_SEC, SECONDS);
            logger.info("{}: {} executor services stopped", NAME, nodeNumber);
        } catch (Exception e) {
            logger.error("{}: {} exception occurred during stopping executor services", NAME, nodeNumber, e);
        }
    }
}
