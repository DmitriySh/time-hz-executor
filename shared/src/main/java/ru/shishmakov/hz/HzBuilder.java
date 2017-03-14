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

/**
 * @author Dmitriy Shishmakov on 12.03.17
 */
public class HzBuilder {

    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final boolean server;
    private final String configFile;

    private boolean kryo;

    private HzBuilder(boolean server) {
        this.server = server;
        this.configFile = server ? "hazelcast.xml" : "hazelcast-client.xml";
    }

    public static HzBuilder instance(boolean isServer) {
        return new HzBuilder(isServer);
    }

    public HzBuilder useKryo() {
        throw new UnsupportedOperationException("Is not supported yet!");
//        this.kryo = true;
//        return this;
    }

    public HazelcastInstance build() {
        final HazelcastInstance hz = server ? buildHZInstance() : buildHZClientInstance();
        final SerializationConfig serialConfig = hz.getConfig().getSerializationConfig();
        return hz;
    }

    private HazelcastInstance buildHZInstance() {
        logger.debug("Load HZ server instance...");
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
