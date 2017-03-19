package ru.shishmakov.core;

import com.hazelcast.core.IMap;
import ru.shishmakov.hz.TimeTask;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Dmitriy Shishmakov on 18.03.17
 */
public class SecondLevelConsumer extends LevelConsumer {

    private static final AtomicInteger iteratorSecondLevel = new AtomicInteger();

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
        this.selfNumber = iteratorSecondLevel.incrementAndGet();
        super.start();
    }
}
