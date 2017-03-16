package ru.shishmakov.config;

import org.aeonbits.owner.Config;

/**
 * @author Dmitriy Shishmakov on 11.03.17
 */
@Config.Sources({"file:config/time.properties", "classpath:config/time.properties"})
public interface TimeConfig extends Config {

    @DefaultValue("250")
    @Key("time.scanIntervalMs")
    int scanIntervalMs();

    @DefaultValue("60000")
    @Key("time.hotLowerRadiusMs")
    int Level200UpperBound();

    @DefaultValue("15000")
    @Key("time.hotUpperRadiusMs")
    int level100UpperBound();

    @DefaultValue("false")
    @Key("schedule.rejectOldTasks")
    boolean isRejectOldTasks();
}
