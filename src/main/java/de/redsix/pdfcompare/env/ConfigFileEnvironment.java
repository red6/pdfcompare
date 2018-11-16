package de.redsix.pdfcompare.env;

import java.io.File;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;

public class ConfigFileEnvironment implements Environment {

    private static final ConfigParseOptions CONFIG_PARSE_OPTIONS = ConfigParseOptions.defaults();

    private final Config config;

    public ConfigFileEnvironment(Path path) {
        Objects.requireNonNull(path, "path is null");

        this.config = ConfigFactory.parseFile(path.toFile(), CONFIG_PARSE_OPTIONS);
    }

    public ConfigFileEnvironment(File file) {
        Objects.requireNonNull(file, "file is null");

        this.config = ConfigFactory.parseFile(file, CONFIG_PARSE_OPTIONS);
    }

    public ConfigFileEnvironment(Reader reader) {
        Objects.requireNonNull(reader, "reader is null");

        this.config = ConfigFactory.parseReader(reader, CONFIG_PARSE_OPTIONS);
    }

    public ConfigFileEnvironment(Config config) {
        Objects.requireNonNull(config, "config is null");

        this.config = config;
    }

    @Override
    public Path getTempDirectory() {
        if (config.hasPath("tempDir")) {
            return Paths.get(config.getString("tempDir"));
        }
        return Paths.get(System.getProperty("java.io.tmpdir"));
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
