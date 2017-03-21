package ru.shishmakov.core;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.aeonbits.owner.ConfigFactory;
import ru.shishmakov.concurrent.ThreadPoolBuilder;
import ru.shishmakov.config.HzConfig;
import ru.shishmakov.config.TimeConfig;
import ru.shishmakov.hz.TimeTask;
import ru.vyarus.guice.ext.ExtAnnotationsModule;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

/**
 * @author Dmitriy Shishmakov on 11.03.17
 */
public class NodeModule extends AbstractModule {

    private static final int QUEUE_CAPACITY = 10_240;

    @Override
    protected void configure() {
        binder().install(new ExtAnnotationsModule());
    }

    @Provides
    @Singleton
    @Named("node.executor")
    public ExecutorService starterExecutor() {
        final int cores = Runtime.getRuntime().availableProcessors();
        return ThreadPoolBuilder.pool("node.executor")
                .withThreads(cores, cores * 4)
                .withSyncQueue()
                .build();
    }

    @Provides
    @Singleton
    @Named("timeQueue.firstLevel")
    public BlockingQueue<TimeTask> queueFirstLevel() {
        return new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    }

    @Provides
    @Singleton
    @Named("timeQueue.secondLevel")
    public BlockingQueue<TimeTask> queueSecondLevel() {
        return new ArrayBlockingQueue<>(QUEUE_CAPACITY);
    }

    @Provides
    @Singleton
    public TimeConfig timeConfig() {
        return ConfigFactory.create(TimeConfig.class);
    }

    @Provides
    @Singleton
    public HzConfig hzConfig() {
        return ConfigFactory.create(HzConfig.class);
    }
}
