package ru.shishmakov.core;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.concurrent.LifeCycle;
import ru.shishmakov.config.HzConfig;
import ru.shishmakov.hz.HzService;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.shishmakov.concurrent.LifeCycle.*;
import static ru.shishmakov.concurrent.Threads.*;

/**
 * @author Dmitriy Shishmakov on 10.03.17
 */
public class Client {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final AtomicReference<LifeCycle> CLIENT_STATE = new AtomicReference<>(IDLE);
    private static final AtomicReference<LifeCycle> SERVICES_STATE = new AtomicReference<>(IDLE);
    private static final CountDownLatch awaitStart = new CountDownLatch(1);
    private static final String CLIENT_SYSTEM_KEY = "client";
    private final int clientNumber;

    @Inject
    private HzConfig hzConfig;
    @Inject
    private HzService hzService;
    @Nullable
    private volatile ServiceManager sm;

    public Client() {
        this.clientNumber = Integer.valueOf(System.getProperty(CLIENT_SYSTEM_KEY, "0"));
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
        new Thread(this::start).start();
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
        startServices(hzService);
        assignThreadHook(this::stop);

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
            stopServices();
            stopExecutors();
        } finally {
            CLIENT_STATE.set(IDLE);
            logger.info("Client: {} stopped, state: {}", clientNumber, CLIENT_STATE.get());
        }
    }

    public void await() throws InterruptedException {
        awaitStart.await();
        logger.info("Node: {} thread: {} await the state: {} to stop itself", clientNumber, Thread.currentThread(), IDLE);
        for (long count = 0; LifeCycle.isNotIdle(CLIENT_STATE.get()); count++) {
            if (count % 100 == 0) logger.debug("Thread: {} is alive", Thread.currentThread());
            sleepWithoutInterrupted(100, MILLISECONDS);
        }
    }

    private void startServices(Service service, Service... services) {
        logger.info("Client: {} services starting...", clientNumber);
        final LifeCycle state = SERVICES_STATE.get();
        if (LifeCycle.isNotIdle(state)) {
            logger.warn("Warning! Client: {} services already started, state: {}", clientNumber, state);
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
        logger.info("Client: {} services started, state: {}", clientNumber, SERVICES_STATE.get());
    }

    private void stopServices() {
        logger.info("Node: {} services stopping...", clientNumber);
        final LifeCycle state = SERVICES_STATE.get();
        if (LifeCycle.isNotRun(state)) {
            logger.warn("Warning! Node: {} services already stopped, state: {}", clientNumber, state);
            return;
        }

        try {
            SERVICES_STATE.set(STOPPING);
            checkNotNull(sm, "Service manager is null").stopAsync().awaitStopped(STOP_TIMEOUT_SEC, SECONDS);
            sm = null;
        } catch (Throwable e) {
            logger.error("Exception occurred during stopping node services", e);
        } finally {
            SERVICES_STATE.set(IDLE);
            logger.info("Node: {} services stopped, state: {}", clientNumber, SERVICES_STATE.get());
        }
    }

    private void stopExecutors() {

    }
}
