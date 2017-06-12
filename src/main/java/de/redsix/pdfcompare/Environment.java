package de.redsix.pdfcompare;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public final class Environment {

    private static Config config;

    static {
        reloadConfig();
    }

    private Environment() {}

    public static Path getTempDirectory() {
        if (config.hasPath("tempDir")) {
            return Paths.get(config.getString("tempDir"));
        }
        return Paths.get(System.getProperty("java.io.tmpdir"));
    }

    public static int getNrOfImagesToCache() {
        return config.getInt("imageCacheSizeCount");
    }

    public static int getMergeCacheSize() {
        return getMB("mergeCacheSizeMB");
    }

    public static int getSwapCacheSize() {
        return getMB("swapCacheSizeMB");
    }

    public static int getDocumentCacheSize() {
        return getMB("documentCacheSizeMB") / 2;
    }

    public static int getMaxImageSize() {
        return config.getInt("maxImageSizeInCache");
    }

    private static int getMB(final String path) {
        return config.getInt(path) * 1024 * 1024;
    }

    public static synchronized void reloadConfig() {
        ConfigFactory.invalidateCaches();
        config = loadConfig();
    }

    private static Config loadConfig() {
        return ConfigFactory.systemEnvironment().withFallback(ConfigFactory.load());
    }

    public static boolean useParallelProcessing() {
        return config.getBoolean("parallelProcessing");
    }
}
