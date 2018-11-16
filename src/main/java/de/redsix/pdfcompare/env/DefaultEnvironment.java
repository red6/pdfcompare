package de.redsix.pdfcompare.env;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class DefaultEnvironment {

    public static Environment create() {
        ConfigFactory.invalidateCaches();
        Config config = ConfigFactory.systemEnvironment().withFallback(ConfigFactory.load());
        return new ConfigFileEnvironment(config);
    }
}
