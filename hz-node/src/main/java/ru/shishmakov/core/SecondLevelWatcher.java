package ru.shishmakov.core;

import com.hazelcast.core.IMap;
import ru.shishmakov.hz.TimeTask;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.BlockingQueue;

/**
 * @author Dmitriy Shishmakov on 16.03.17
 */
@Singleton
public class SecondLevelWatcher extends LevelWatcher {

    @Inject
    @Named("timeQueue.secondLevel")
    private BlockingQueue<TimeTask> queueSecondLevel;
    private IMap<Long, TimeTask> mapSecondLevel;

    @Override
    protected BlockingQueue<TimeTask> getQueue() {
        return queueSecondLevel;
    }

    @Override
    protected IMap<Long, TimeTask> getIMap() {
        return mapSecondLevel;
    }

    @Override
    public void start() {
        this.mapSecondLevel = hzObjects.getSecondLevelMap();
        super.start();
    }

}


















