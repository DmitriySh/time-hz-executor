package ru.shishmakov.node;

import com.google.inject.Guice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.Shared;
import ru.shishmakov.config.TimeConfig;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutorService;

/**
 * @author Dmitriy Shishmakov on 10.03.17
 */
public class Node {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private TimeConfig timeConfig;

    @Inject
    @Named("node.executor")
    private ExecutorService executor;

    public static void main(String[] args) {
        System.out.println(new Shared("Node Shared"));
        Guice.createInjector(new NodeModule())
                .getInstance(Node.class)
                .start();
    }

    private void start() {
        logger.info("Hz node starting ...");

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            logger.error("Error", e);
        }
//        logger.debug("executor: {}", executor);
//        logger.debug("scanIntervalMs: {}, hotRadiusMs: {}", timeConfig.scanIntervalMs(), timeConfig.hotRadiusMs());
    }
}
