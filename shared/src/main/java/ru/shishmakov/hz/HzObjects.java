package ru.shishmakov.hz;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.invoke.MethodHandles;

/**
 * @author Dmitriy Shishmakov on 16.03.17
 */
@Singleton
public class HzObjects {
    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final HazelcastInstance hzInstance;

    @Inject
    public HzObjects(HzService hzService) {
        this.hzInstance = hzService.getHzInstance();
    }

    private enum Maps {
        LEVEL_100,
        LEVEL_200
    }

    private enum IdGenerators {
        ID
    }

    public IdGenerator getIdGenerator() {
        return hzInstance.getIdGenerator(IdGenerators.ID.name());
    }

    public IMap<Long, TaskTime> getLevel100Map() {
        return hzInstance.getMap(Maps.LEVEL_100.name());
    }

    public IMap<Long, TaskTime> getLevel200Map() {
        return hzInstance.getMap(Maps.LEVEL_200.name());
    }
}
