package de.redsix.pdfcompare.env;

import java.io.File;

public interface Environment {

	File getTempDirectory();

    int getNrOfImagesToCache();

    int getMergeCacheSize();

    int getSwapCacheSize();

    int getDocumentCacheSize();

    int getMaxImageSize();

    int getOverallTimeout();

    boolean useParallelProcessing();

    double getAllowedDiffInPercent();

}
