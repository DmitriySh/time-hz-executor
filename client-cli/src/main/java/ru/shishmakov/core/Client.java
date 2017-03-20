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
public class Client {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Logger ucLogger = LoggerFactory.getLogger("userConsole");

    private static final String NAME = MethodHandles.lookup().lookupClass().getSimpleName();
    private static final AtomicReference<LifeCycle> CLIENT_STATE = new AtomicReference<>(IDLE);
    private static final CountDownLatch awaitStart = new CountDownLatch(1);
    private static final String CLIENT_SYSTEM_KEY = "client";

    @Inject
    @Named("client.executor")
    private ExecutorService executor;
    @Inject
    private ConsoleClient consoleClient;
    @Inject
    private HzService hzService;
    @Inject
    private ServiceController serviceController;
    private final int clientNumber;


    public Client() {
        this.clientNumber = Integer.valueOf(System.getProperty(CLIENT_SYSTEM_KEY, "0"));
    }

    @PostConstruct
    public void setUp() {
        logger.info("----- // -----    {}: {} START {}    ----- // -----", NAME, clientNumber, LocalDateTime.now());
        ucLogger.info("{}: {} START {}", NAME, clientNumber, LocalDateTime.now());
        this.serviceController.setMetaInfo(clientNumber, "Client");
        this.consoleClient.setMetaInfo(clientNumber, NAME);
    }

    @PreDestroy
    public void tearDown() {
        logger.info("----- // -----    {}: {} STOP {}    ----- // -----", NAME, clientNumber, LocalDateTime.now());
        ucLogger.info("{}: {} STOP {}\nBuy!", NAME, clientNumber, LocalDateTime.now());
    }

    public Client startAsync() {
        new Thread(this::start, "client-hz-" + clientNumber).start();
        return this;
    }

    public Client start() {
        logger.info("{}: {} starting...", NAME, clientNumber);

        final LifeCycle state = CLIENT_STATE.get();
        if (LifeCycle.isNotIdle(state)) {
            logger.warn("Warning! {}: {} already started, state: {}", NAME, clientNumber, state);
            return this;
        }
        CLIENT_STATE.set(INIT);
        awaitStart.countDown();
        serviceController.startServices(hzService, consoleClient);
        assignThreadHook(this::stop, "client-" + clientNumber + "-hook-thread");

        CLIENT_STATE.set(RUN);
        logger.info("{}: {} started, state: {}", NAME, clientNumber, CLIENT_STATE.get());
        return this;
    }

    public void stop() {
        logger.info("{}: {} stopping...", NAME, clientNumber);
        final LifeCycle state = CLIENT_STATE.get();
        if (LifeCycle.isNotRun(state)) {
            logger.warn("Warning! {}: {} already stopped, state: {}", NAME, clientNumber, state);
            return;
        }

        try {
            CLIENT_STATE.set(STOPPING);
            serviceController.stopServices();
            stopExecutors();
        } finally {
            CLIENT_STATE.set(IDLE);
            logger.info("{}: {} stopped, state: {}", NAME, clientNumber, CLIENT_STATE.get());
            ucLogger.info("{}: {} stopped, state: {}", NAME, clientNumber, CLIENT_STATE.get());
        }
    }

    public void await() throws InterruptedException {
        awaitStart.await();
        Thread.currentThread().setName("client-main-" + clientNumber);
        logger.info("{}: {} thread: {} await the state: {} to stop itself", NAME, clientNumber, Thread.currentThread(), IDLE);
        for (long count = 0; LifeCycle.isNotIdle(CLIENT_STATE.get()); count++) {
            if (count % 100 == 0) logger.debug("Thread: {} is alive", Thread.currentThread());
            sleepWithoutInterruptedAfterTimeout(100, MILLISECONDS);
        }
    }


    private void stopExecutors() {
        logger.info("{}: {} executor services stopping...", NAME, clientNumber);
        try {
            MoreExecutors.shutdownAndAwaitTermination(executor, STOP_TIMEOUT_SEC, SECONDS);
            logger.info("Executor services stopped");
        } catch (Exception e) {
            logger.error("{}: {} exception occurred during stopping executor services", NAME, clientNumber, e);
        }
    }
}
