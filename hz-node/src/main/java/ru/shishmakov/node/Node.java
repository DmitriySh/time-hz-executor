package ru.shishmakov.node;

import com.google.inject.Guice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.Shared;

import java.lang.invoke.MethodHandles;

/**
 * @author Dmitriy Shishmakov on 10.03.17
 */
public class Node {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args) {
        System.out.println(new Shared("Node Shared"));
        Guice.createInjector(new NodeModule())
                .getInstance(Node.class)
                .start();
    }

    private void start() {
        logger.info("Hz node starting ...");
    }
}
