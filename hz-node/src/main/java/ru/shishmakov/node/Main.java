package ru.shishmakov.node;

import com.google.inject.Guice;
import ru.shishmakov.core.Node;
import ru.shishmakov.core.NodeModule;

/**
 * @author Dmitriy Shishmakov on 11.03.17
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Guice.createInjector(new NodeModule())
                .getInstance(Node.class)
                .startAsync().await();
    }
}
