package ru.shishmakov.hz;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.concurrent.Callable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dmitriy Shishmakov on 16.03.17
 */
public abstract class HzCallable<T> implements Callable<T>, Serializable, HazelcastInstanceAware {
    @Nullable
    private transient HazelcastInstance hz;

    @Override
    public void setHazelcastInstance(HazelcastInstance hz) {
        this.hz = checkNotNull(hz, "hazelcast instance is null");
    }

    @Nullable
    public HazelcastInstance getHz() {
        return checkNotNull(hz, "hazelcast instance is null");
    }
}
