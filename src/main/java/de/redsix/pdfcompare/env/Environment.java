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

    boolean useParallelProcessing();

    double getAllowedDiffInPercent();

    Color getExpectedColor();

    Color getActualColor();

    int getDPI();
}
