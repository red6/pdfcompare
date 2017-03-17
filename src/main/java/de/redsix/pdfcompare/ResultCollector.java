package de.redsix.pdfcompare;

import java.awt.image.BufferedImage;

public interface ResultCollector {

    void addPage(boolean hasDifferences, boolean hasDifferenceInExclusion, int pageIndex,
            BufferedImage expectedImage, BufferedImage actualImage, BufferedImage diffImage);

    void noPagesFound();

    default void done() {}
}
