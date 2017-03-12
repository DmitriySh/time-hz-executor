package ru.shishmakov.node;

import com.google.inject.Guice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.core.Node;
import ru.shishmakov.core.NodeModule;

import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;

/**
 * @author Dmitriy Shishmakov on 11.03.17
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    public static void main(String[] args) {
        logger.info("----- // -----    NODE START {}    ----- // -----", LocalDateTime.now());
        Guice.createInjector(new NodeModule())
                .getInstance(Node.class)
                .startAsync().await();
    }
}
