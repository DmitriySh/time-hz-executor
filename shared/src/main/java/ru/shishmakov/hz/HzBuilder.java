package ru.shishmakov.hz;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.config.ClasspathXmlConfig;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Dmitriy Shishmakov on 12.03.17
 */
public class HzBuilder {

    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final boolean server;
    private final String configFile;

    private boolean kryo;

    private HzBuilder(boolean server, String configFile) {
        this.server = server;
        this.configFile = checkNotNull(configFile, "config file should not be null");
    }

    public static HzBuilder serverInstance() {
        return new HzBuilder(true, "hazelcast.xml");
    }

    public static HzBuilder clientInstance() {
        return new HzBuilder(false, "hazelcast-client.xml");
    }

    public HzBuilder useKryo() {
        this.kryo = true;
        return this;
    }

    public HazelcastInstance build() {
        final HazelcastInstance hz = server ? buildHZInstance() : buildHZClientInstance();
        SerializationConfig serializationConfig = hz.getConfig().getSerializationConfig();
        return hz;
    }

    private HazelcastInstance buildHZInstance() {
        logger.debug("Load HZ instance...");
        return Hazelcast.newHazelcastInstance(new ClasspathXmlConfig(configFile));
    }

    private HazelcastInstance buildHZClientInstance() {
        logger.debug("Load HZ client instance...");
        try {
            return HazelcastClient.newHazelcastClient(new XmlClientConfigBuilder(configFile).build());
        } catch (IOException e) {
            throw new IllegalStateException("Could not load client config file", e);
        }
    }


}
