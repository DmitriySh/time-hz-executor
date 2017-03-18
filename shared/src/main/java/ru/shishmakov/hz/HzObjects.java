package ru.shishmakov.hz;

import com.hazelcast.core.IMap;
import com.hazelcast.core.IdGenerator;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Dmitriy Shishmakov on 16.03.17
 */
@Singleton
public class HzObjects {

    @Inject
    private HzService hzService;

    private enum Maps {
        FIRST_LEVEL,
        SECOND_LEVEL
    }

    private enum IdGenerators {
        TASK_ID
    }

    public long getClusterTime() {
        return hzService.getHzInstance().getCluster().getClusterTime();
    }

    public IdGenerator getTaskIdGenerator() {
        return hzService.getHzInstance().getIdGenerator(IdGenerators.TASK_ID.name());
    }

    public IMap<Long, TimeTask> getFirstLevelMap() {
        return hzService.getHzInstance().getMap(Maps.FIRST_LEVEL.name());
    }

    public IMap<Long, TimeTask> getSecondLevelMap() {
        return hzService.getHzInstance().getMap(Maps.SECOND_LEVEL.name());
    }
}
