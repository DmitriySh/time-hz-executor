package ru.shishmakov.hz;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IdGenerator;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Dmitriy Shishmakov on 16.03.17
 */
@Singleton
public class HzDOController {

    private final HazelcastInstance hzInstance;

    @Inject
    public HzDOController(HzService hzService) {
        this.hzInstance = hzService.getHzInstance();
    }

    private enum Maps {
        FIRST_LEVEL,
        SECOND_LEVEL
    }

    private enum IdGenerators {
        TASK_ID
    }

    public IdGenerator getTaskIdGenerator() {
        return hzInstance.getIdGenerator(IdGenerators.TASK_ID.name());
    }

    public IMap<Long, TaskTime> getFirstLevelMap() {
        return hzInstance.getMap(Maps.FIRST_LEVEL.name());
    }

    public IMap<Long, TaskTime> getSecondLevelMap() {
        return hzInstance.getMap(Maps.SECOND_LEVEL.name());
    }
}
