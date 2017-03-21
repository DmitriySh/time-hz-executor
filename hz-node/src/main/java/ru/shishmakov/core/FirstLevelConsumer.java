package ru.shishmakov.core;

import com.hazelcast.core.IMap;
import ru.shishmakov.hz.TimeTask;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * First level is a subset of hot tasks retrieve from {@link BlockingQueue} and execute independent of other part of tasks
 *
 * @author Dmitriy Shishmakov on 18.03.17
 */
public class FirstLevelConsumer extends LevelConsumer {

    private static final AtomicInteger iteratorFirstLevel = new AtomicInteger();

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
        this.selfNumber = iteratorFirstLevel.incrementAndGet();
        super.start();
    }
}
