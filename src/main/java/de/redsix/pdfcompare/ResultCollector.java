package de.redsix.pdfcompare;

import java.util.List;

public interface ResultCollector {

    /**
     * Records one compared page.
     *
     * @param resultPages the pages to write to the result document for this page, in order. One page normally,
     *                    two (expected, then actual) when horizontal compare output is enabled.
     */
    void addPage(PageDiffCalculator diffCalculator, int pageIndex,
            ImageWithDimension expectedImage, ImageWithDimension actualImage, List<ImageWithDimension> resultPages);

    void noPagesFound();

    default void done() {}
}
