package ru.shishmakov.hz;

import com.google.common.base.Stopwatch;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.apache.commons.lang3.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.shishmakov.config.HzConfig;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.shishmakov.concurrent.Threads.sleepWithInterruptedAfterTimeout;

/**
 * @author Dmitriy Shishmakov on 12.03.17
 */
public class HzBuilder {
    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final HzConfig hzConfig;
    private final String configFile;
    private boolean kryo;

    private HzBuilder(HzConfig hzConfig) {
        this.hzConfig = hzConfig;
        this.configFile = hzConfig.server() ? "hazelcast.xml" : "hazelcast-client.xml";
    }

    public static HzBuilder instance(HzConfig hzConfig) {
        return new HzBuilder(hzConfig);
    }

    public HzBuilder withKryo() {
        throw new UnsupportedOperationException("kryo is not supported yet!");
//        this.kryo = true;
//        return this;
    }

    public HazelcastInstance build() throws InterruptedException {
        return hzConfig.server() ? buildHZInstance() : buildHZClientInstance();
    }

    private HazelcastInstance buildHZInstance() {
        logger.debug("Load HZ server instance...");
        return Hazelcast.newHazelcastInstance(new ClasspathXmlConfig(configFile));
    }

    private HazelcastInstance buildHZClientInstance() throws InterruptedException {
        logger.debug("Load HZ client instance...");
        try {
            final HazelcastInstance instance = HazelcastClient.newHazelcastClient(new XmlClientConfigBuilder(configFile).build());
            awaitClusterQuorum(instance);
            return instance;
        } catch (IOException e) {
            throw new IllegalStateException("Could not load client config file", e);
        }
    }

    private void awaitClusterQuorum(HazelcastInstance client) throws InterruptedException {
        if (hzConfig.clientMinClusterSize() <= 0) return;

        final Range<Long> timeout = Range.between(0L, SECONDS.toMillis(hzConfig.clientInitialWaitTimeoutSec()));
        final Stopwatch sw = Stopwatch.createStarted();
        while (client.getCluster().getMembers().size() < hzConfig.clientMinClusterSize()) {
            logger.debug("Client await cluster quorum... found: {}, need: {}", client.getCluster().getMembers().size(),
                    hzConfig.clientMinClusterSize());
            sleepWithInterruptedAfterTimeout(1, SECONDS);
            if (timeout.isBefore(sw.elapsed(MILLISECONDS))) {
                throw new RuntimeException("Wait timeout is exceeded, elapsed: " + sw.elapsed(MILLISECONDS));
            }
        }
        logger.info("Client wasted time to have a cluster quorum: {}", sw);
    }
}
