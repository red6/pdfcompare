package de.redsix.pdfcompare.env;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;

import java.awt.*;
import java.io.File;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * This environment uses Lightbend Config to read the configuration data.
 * You can either provide a Lightbend Config object or give a File, Path or Reader where
 * the config file can be read.
 * System environment variables always take precedence over configurations from a file.
 * The default configuration is also always read.
 */
public class ConfigFileEnvironment implements Environment {

    private static final ConfigParseOptions CONFIG_PARSE_OPTIONS = ConfigParseOptions.defaults();

    private final Config config;

    public ConfigFileEnvironment(Path path) {
        Objects.requireNonNull(path, "path is null");

        this.config = ConfigFactory.systemEnvironment()
                .withFallback(ConfigFactory.parseFile(path.toFile(), CONFIG_PARSE_OPTIONS))
                .withFallback(ConfigFactory.load());
    }

    public ConfigFileEnvironment(File file) {
        Objects.requireNonNull(file, "file is null");

        this.config = ConfigFactory.systemEnvironment()
                .withFallback(ConfigFactory.parseFile(file, CONFIG_PARSE_OPTIONS))
                .withFallback(ConfigFactory.load());
    }

    public ConfigFileEnvironment(Reader reader) {
        Objects.requireNonNull(reader, "reader is null");

        this.config = ConfigFactory.systemEnvironment()
                .withFallback(ConfigFactory.parseReader(reader, CONFIG_PARSE_OPTIONS))
                .withFallback(ConfigFactory.load());
    }

    public ConfigFileEnvironment(Config config) {
        Objects.requireNonNull(config, "config is null");

        this.config = ConfigFactory.systemEnvironment()
                .withFallback(config);
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
    public int getExecutorTimeout() {
        if (config.hasPath("executorTimeoutInSeconds")) {
            return config.getInt("executorTimeoutInSeconds");
        }
        return 60;
    }

    @Override
    public boolean useParallelProcessing() {
        return config.getBoolean("parallelProcessing");
    }

    @Override
    public double getAllowedDiffInPercent() {
        return config.getDouble("allowedDifferenceInPercentPerPage");
    }

    @Override
    public Color getExpectedColor() {
        if (config.hasPath("expectedColor")) {
            return Color.decode("#" + config.getString("expectedColor"));
        }
        return new Color(0, 180, 0);
    }

    @Override
    public Color getActualColor() {
        if (config.hasPath("actualColor")) {
            return Color.decode("#" + config.getString("actualColor"));
        }
        return new Color(210, 0, 0);
    }

    @Override
    public int getDPI() {
        return config.getInt("DPI");
    }

    @Override
    public boolean addEqualPagesToResult() {
        return config.getBoolean("addEqualPagesToResult");
    }

    @Override
    public boolean failOnMissingIgnoreFile() {
        return config.getBoolean("failOnMissingIgnoreFile");
    }

    private int getMB(final String path) {
        return config.getInt(path) * 1024 * 1024;
    }
}
