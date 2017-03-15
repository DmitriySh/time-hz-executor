package ru.shishmakov.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.concurrent.LifeCycle;
import ru.shishmakov.concurrent.ServiceController;
import ru.shishmakov.hz.HzService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ru.shishmakov.concurrent.LifeCycle.*;
import static ru.shishmakov.concurrent.Threads.assignThreadHook;
import static ru.shishmakov.concurrent.Threads.sleepWithoutInterrupted;

/**
 * @author Dmitriy Shishmakov on 10.03.17
 */
public class Client {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final AtomicReference<LifeCycle> CLIENT_STATE = new AtomicReference<>(IDLE);
    private static final CountDownLatch awaitStart = new CountDownLatch(1);
    private static final String CLIENT_SYSTEM_KEY = "client";
    private final int clientNumber;

    @Inject
    private HzService hzService;
    private final ServiceController serviceController;


    public Client() {
        this.clientNumber = Integer.valueOf(System.getProperty(CLIENT_SYSTEM_KEY, "0"));
        this.serviceController = new ServiceController(clientNumber, "Client");
    }

    @PostConstruct
    public void setUp() {
        logger.info("----- // -----    CLIENT: {} START {}    ----- // -----", clientNumber, LocalDateTime.now());
    }

    @PreDestroy
    public void tearDown() {
        logger.info("----- // -----    CLIENT: {} STOP {}    ----- // -----", clientNumber, LocalDateTime.now());
    }

    public Client startAsync() {
        new Thread(this::start, "client-hz-" + clientNumber).start();
        return this;
    }

    public Client start() {
        logger.info("Client: {} starting...", clientNumber);

        final LifeCycle state = CLIENT_STATE.get();
        if (LifeCycle.isNotIdle(state)) {
            logger.warn("Warning! Client: {} already started, state: {}", clientNumber, state);
            return this;
        }
        CLIENT_STATE.set(INIT);
        awaitStart.countDown();
        serviceController.startServices(hzService);
        assignThreadHook(this::stop, "client-" + clientNumber + "-hook-thread");

        CLIENT_STATE.set(RUN);
        logger.info("Client: {} started, state: {}", clientNumber, CLIENT_STATE.get());
        return this;
    }

    public void stop() {
        logger.info("Client: {} stopping...", clientNumber);
        final LifeCycle state = CLIENT_STATE.get();
        if (LifeCycle.isNotRun(state)) {
            logger.warn("Warning! Client: {} already stopped, state: {}", clientNumber, state);
            return;
        }

        try {
            CLIENT_STATE.set(STOPPING);
            serviceController.stopServices();
            stopExecutors();
        } finally {
            CLIENT_STATE.set(IDLE);
            logger.info("Client: {} stopped, state: {}", clientNumber, CLIENT_STATE.get());
        }
    }

    public void await() throws InterruptedException {
        awaitStart.await();
        Thread.currentThread().setName("client-main-" + clientNumber);
        logger.info("Client: {} thread: {} await the state: {} to stop itself", clientNumber, Thread.currentThread(), IDLE);
        for (long count = 0; LifeCycle.isNotIdle(CLIENT_STATE.get()); count++) {
            if (count % 100 == 0) logger.debug("Thread: {} is alive", Thread.currentThread());
            sleepWithoutInterrupted(100, MILLISECONDS);
        }
    }


    private void stopExecutors() {

    }
}
