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
public class FirstLevelWatcher extends LevelWatcher {

    @Inject
    @Named("timeQueue.firstLevel")
    private BlockingQueue<TimeTask> queueFirstLevel;
    private IMap<Long, TimeTask> mapFirstLevel;

    @Override
    protected BlockingQueue<TimeTask> getQueue() {
        return queueFirstLevel;
    }

    @Override
    protected IMap<Long, TimeTask> getIMap() {
        return mapFirstLevel;
    }

    @Override
    public void start() {
        this.mapFirstLevel = hzObjects.getFirstLevelMap();
        super.start();
    }

}
