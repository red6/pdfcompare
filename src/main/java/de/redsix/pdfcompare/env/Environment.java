package de.redsix.pdfcompare.env;

import java.awt.*;
import java.nio.file.Path;

public interface Environment {

    Path getTempDirectory();

    int getNrOfImagesToCache();

    int getMergeCacheSize();

    int getSwapCacheSize();

    int getDocumentCacheSize();

    int getMaxImageSize();

    int getOverallTimeout();

    int getExecutorTimeout();

    boolean useParallelProcessing();

    double getAllowedDiffInPercent();

    Color getExpectedColor();

    Color getActualColor();

    int getDPI();

    /**
     * The maximum number of pixels a single page may render to at {@link #getDPI()}.
     * Larger pages are rejected before any image memory is allocated.
     * The diff image of a page pair may have up to twice as many pixels.
     * Implementations written before this setting existed get the same default as {@code reference.conf}.
     */
    default long getMaxPagePixels() {
        return 150_000_000L;
    }

    boolean addEqualPagesToResult();

    boolean failOnMissingIgnoreFile();
    
    boolean getEnableHorizontalCompareOutput();
}
