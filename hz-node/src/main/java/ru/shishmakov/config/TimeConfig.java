package ru.shishmakov.config;

import org.aeonbits.owner.Config;

/**
 * @author Dmitriy Shishmakov on 11.03.17
 */
@Config.Sources({"file:config/time.properties", "classpath:config/time.properties"})
public interface TimeConfig extends Config {

    @DefaultValue("250")
    @Key("time.scanIntervalMs")
    long scanIntervalMs();

    @DefaultValue("120000")
    @Key("time.secondUpperBoundMs")
    long secondLevelUpperBound();

    @DefaultValue("10000")
    @Key("time.firstUpperBoundMs")
    long firstLevelUpperBound();

    @DefaultValue("false")
    @Key("schedule.rejectOldTasks")
    boolean isRejectOldTasks();
}
