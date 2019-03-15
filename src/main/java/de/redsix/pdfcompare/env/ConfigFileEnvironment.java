package de.redsix.pdfcompare.env;

import static org.apache.commons.lang3.Validate.notNull;

import java.io.File;
import java.io.Reader;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;

public class ConfigFileEnvironment implements Environment {

    private static final ConfigParseOptions CONFIG_PARSE_OPTIONS = ConfigParseOptions.defaults();

    private final Config config;

    public ConfigFileEnvironment(File file) {
        notNull(file, "file is null");
        this.config = ConfigFactory.parseFile(file, CONFIG_PARSE_OPTIONS);
    }

    public ConfigFileEnvironment(Reader reader) {
        notNull(reader, "reader is null");
        this.config = ConfigFactory.parseReader(reader, CONFIG_PARSE_OPTIONS);
    }

    public ConfigFileEnvironment(Config config) {
        notNull(config, "config is null");
        this.config = config;
    }

    @Override
    public File getTempDirectory() {
        if (config.hasPath("tempDir")) {
            return new File(config.getString("tempDir"));
        }
        return new File(System.getProperty("java.io.tmpdir"));
    }

    @Override
    public int getNrOfImagesToCache() {
        return config.getInt("imageCacheSizeCount");
    }

    @Override
    public int getMergeCacheSize() {
        return getMB("mergeCacheSizeMB");
    }

    @Override
    public int getSwapCacheSize() {
        return getMB("swapCacheSizeMB");
    }

    @Override
    public int getDocumentCacheSize() {
        return getMB("documentCacheSizeMB") / 2;
    }

    @Override
    public int getMaxImageSize() {
        return config.getInt("maxImageSizeInCache");
    }

    @Override
    public int getOverallTimeout() {
        return config.getInt("overallTimeoutInMinutes");
    }

    @Override
    public boolean useParallelProcessing() {
        return config.getBoolean("parallelProcessing");
    }

    @Override
    public double getAllowedDiffInPercent() {
        if (config.hasPath("allowedDifferenceInPercentPerPage")) {
            return config.getDouble("allowedDifferenceInPercentPerPage");
        }
        return 0;
    }

    private int getMB(final String path) {
        return config.getInt(path) * 1024 * 1024;
    }
}
