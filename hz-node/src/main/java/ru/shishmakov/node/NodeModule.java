package ru.shishmakov.node;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import ru.shishmakov.concurrent.ThreadPools;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;

/**
 * @author Dmitriy Shishmakov on 11.03.17
 */
public class NodeModule extends AbstractModule {

    @Override
    protected void configure() {

    }

    @Provides
    @Singleton
    @Named("node.executor")
    public ExecutorService starterExecutor() {
        final int cores = Runtime.getRuntime().availableProcessors();
        return ThreadPools.buildThreadPool("node.executor", cores, cores * 4);
    }
}
