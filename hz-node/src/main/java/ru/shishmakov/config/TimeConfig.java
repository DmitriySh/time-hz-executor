package ru.shishmakov.config;

import org.aeonbits.owner.Config;

/**
 * @author Dmitriy Shishmakov on 11.03.17
 */
@Config.Sources({"file:config/time.properties", "classpath:config/time.properties"})
public interface TimeConfig extends Config {

    @DefaultValue("150")
    @Key("time.scanIntervalMs")
    int scanIntervalMs();

    @DefaultValue("5000")
    @Key("time.hotRadiusMs")
    int hotRadiusMs();

    @DefaultValue("false")
    @Key("schedule.rejectOldTasks")
    boolean isRejectOldTasks();
}
