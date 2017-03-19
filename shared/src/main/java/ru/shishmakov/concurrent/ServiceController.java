package ru.shishmakov.concurrent;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.shishmakov.concurrent.LifeCycle.*;
import static ru.shishmakov.concurrent.Threads.STOP_TIMEOUT_SEC;

/**
 * @author Dmitriy Shishmakov on 14.03.17
 */
public class ServiceController {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final AtomicReference<LifeCycle> SERVICES_STATE = new AtomicReference<>(IDLE);
    @Nullable
    private volatile ServiceManager sm;
    private final int ownerNumber;
    private final String ownerName;

    public ServiceController(int ownerNumber, String ownerName) {
        this.ownerNumber = ownerNumber;
        this.ownerName = ownerName;
    }

    public void startServices(Service service, Service... services) {
        logger.info("{}:{} services starting...", ownerName, ownerNumber);
        final LifeCycle state = SERVICES_STATE.get();
        if (LifeCycle.isNotIdle(state)) {
            logger.warn("Warning! {}:{} services already started, state: {}", ownerName, ownerNumber, state);
            return;
        }

        try {
            SERVICES_STATE.set(INIT);
            final ServiceManager sm = new ServiceManager(Lists.asList(service, services));
            sm.addListener(buildServiceListener(), MoreExecutors.directExecutor());
            sm.startAsync().awaitHealthy();
            this.sm = sm;
        } catch (Throwable e) {
            logger.error("Exception occurred during starting node services", e);
        } finally {
            SERVICES_STATE.set(RUN);
            logger.info("{}:{} services started, state: {}", ownerName, ownerNumber, SERVICES_STATE.get());
        }
    }

    public void stopServices() {
        logger.info("{}:{} services stopping...", ownerName, ownerNumber);
        final LifeCycle state = SERVICES_STATE.get();
        if (LifeCycle.isNotRun(state)) {
            logger.warn("Warning! {}:{} services already stopped, state: {}", ownerName, ownerNumber, state);
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
            logger.info("{}:{} services stopped, state: {}", ownerName, ownerNumber, SERVICES_STATE.get());
        }
    }

    private ServiceManager.Listener buildServiceListener() {
        return new ServiceManager.Listener() {
            @Override
            public void healthy() {
                logger.info("Listener: {}:{} has started all services  -->", ownerName, ownerNumber);
            }

            @Override
            public void stopped() {
                logger.info("Listener: {}:{} has stopped all services  <--", ownerName, ownerNumber);
            }

            @Override
            public void failure(Service service) {
                logger.error("Error! {}:{} service: {} has crashed  X--X", ownerName, ownerNumber, service, service.failureCause());
            }
        };
    }
}
