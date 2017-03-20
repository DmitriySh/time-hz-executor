package ru.shishmakov.core;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.aeonbits.owner.ConfigFactory;
import ru.shishmakov.concurrent.ThreadPoolBuilder;
import ru.shishmakov.config.HzConfig;
import ru.vyarus.guice.ext.ExtAnnotationsModule;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;

/**
 * @author Dmitriy Shishmakov on 13.03.17
 */
public class ClientModule extends AbstractModule {

    @Override
    protected void configure() {
        binder().install(new ExtAnnotationsModule());
    }

    @Provides
    @Singleton
    @Named("client.executor")
    public ExecutorService starterExecutor() {
        return ThreadPoolBuilder.pool("client.executor")
                .withThreads(1)
                .build();
    }

    @Provides
    @Singleton
    public HzConfig hzConfig() {
        return ConfigFactory.create(HzConfig.class);
    }
}
