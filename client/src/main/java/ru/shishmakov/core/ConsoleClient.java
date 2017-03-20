package ru.shishmakov.core;

import com.google.common.base.Splitter;
import com.google.common.util.concurrent.AbstractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.config.HzConfig;
import ru.shishmakov.hz.HzService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author Dmitriy Shishmakov on 20.03.17
 */
public class ConsoleClient extends AbstractService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Logger ucLogger = LoggerFactory.getLogger("userConsole");

    private static final String NAME = MethodHandles.lookup().lookupClass().getSimpleName();
    @Inject
    @Named("client.executor")
    private ExecutorService executor;
    @Inject
    private HzService hzService;
    @Inject
    private HzConfig hzConfig;
    @Inject
    private Provider<Client> client;

    private final AtomicBoolean watcherState = new AtomicBoolean(true);
    private int ownerNumber;
    private String ownerName;

    public void setMetaInfo(int ownerNumber, String ownerName) {
        this.ownerNumber = ownerNumber;
        this.ownerName = ownerName;
    }

    @Override
    protected void doStart() {
        logger.info("{} {}:{} starting...", NAME, ownerName, ownerNumber);
        try {
            startClientService();
            notifyStarted();
            logger.info("{} {}:{} started", NAME, ownerName, ownerNumber);
        } catch (Throwable e) {
            notifyFailed(e);
        }
    }

    @Override
    protected void doStop() {
        logger.info("{} {}:{} stopping...", NAME, ownerName, ownerNumber);
        try {
            stopClientService();
            notifyStopped();
            logger.info("{} {}:{} stopped", NAME, ownerName, ownerNumber);
        } catch (Throwable e) {
            notifyFailed(e);
        }
    }

    private void startClientService() throws TimeoutException {
        hzService.awaitRunning(hzConfig.clientInitialWaitTimeoutSec(), SECONDS);
        executor.execute(this::process);
    }

    private void stopClientService() {
        shutdownClient();
    }

    private void process() {
        logger.info("{} {}:{} listening user typing...", NAME, ownerName, ownerNumber);
        ucLogger.info("{}: {} get ready, choose command... (/h - help)", NAME, ownerName); // ???
        try {
            try (BufferedReader input = new BufferedReader(new InputStreamReader(System.in))) {
                while (watcherState.get() && !Thread.currentThread().isInterrupted()) {
                    final String read = input.readLine();
                    if (isBlank(read)) continue;

                    final Iterator<String> it = Splitter.on(' ').split(read).iterator();
                    if (!it.hasNext()) continue;

                    final String line = it.next();
                    if (isBlank(line)) continue;

                    logger.debug("{} {}:{} user typed: {}", NAME, ownerName, ownerNumber, line);

                    if (equalsIgnoreCase(line, "/h") || equalsIgnoreCase(line, "/help")) {
                        ucLogger.info(String.format("\t%s - %s%n\t%s%n", "h", "help", "You see current message"));
                        ucLogger.info(String.format("\t%s - %s%n\t%s%n", "s",
                                "send <local_date_time_pattern>:<yyyy-MM-ddTHH:mm> <message>:<string>",
                                "You send the text message at the scheduled time to execute on Hazelcast node"));
                        ucLogger.info(String.format("\t%s - %s%n\t%s%n", "q", "quit", "End session and quit"));
                        ucLogger.info("Start your command with slash symbol '/'\nAuthor: Dmitriy Shishmakov\n");
                        continue;
                    }

                    if (equalsIgnoreCase(line, "/q") || equalsIgnoreCase(line, "/quit")) {
                        client.get().stop();
                        break;
                    }
                    if (equalsIgnoreCase(line, "/s") || equalsIgnoreCase(line, "/send")) {
                        continue;
                    }
                }
            }

        } catch (Exception e) {
            logger.error("{} {}:{} error in time of processing", NAME, ownerName, ownerNumber, e);
        } finally {
            shutdownClient();
        }
    }

    private void shutdownClient() {
        if (watcherState.compareAndSet(true, false)) {
            logger.debug("{} {}:{} waiting for shutdown the client...", NAME, ownerName, ownerNumber);
        }

    }
}
