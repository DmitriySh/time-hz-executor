package ru.shishmakov.core;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.aeonbits.owner.ConfigFactory;
import ru.shishmakov.config.HzConfig;
import ru.vyarus.guice.ext.ExtAnnotationsModule;

import javax.inject.Singleton;

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
    public HzConfig hzConfig() {
        return ConfigFactory.create(HzConfig.class);
    }
}
