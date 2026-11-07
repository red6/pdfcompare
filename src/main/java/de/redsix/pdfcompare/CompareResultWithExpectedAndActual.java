package de.redsix.pdfcompare;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A CompareResult, that also stores the expected and actual Image and also keeps diffImages in memory for later display.
 */
public class CompareResultWithExpectedAndActual extends CompareResultImpl {

    private final Map<Integer, ImageWithDimension> expectedImages = new TreeMap<>();
    private final Map<Integer, ImageWithDimension> actualImages = new TreeMap<>();

    @Override
    public synchronized void addPage(final PageDiffCalculator diffCalculator, final int pageIndex,
            final ImageWithDimension expectedImage, final ImageWithDimension actualImage, final List<ImageWithDimension> resultPages) {
        super.addPage(diffCalculator, pageIndex, expectedImage, actualImage, resultPages);
        expectedImages.put(pageIndex, expectedImage);
        actualImages.put(pageIndex, actualImage);
    }

    @Override
    protected boolean keepImages() {
        return true;
    }

    /**
     * The first result page of the given page. With horizontal compare output that is the tinted expected page.
     */
    public synchronized BufferedImage getDiffImage(final int page) {
        final List<ImageWithDimension> resultPages = diffImages.get(page);
        return resultPages == null ? null : resultPages.get(0).bufferedImage;
    }

    public BufferedImage getExpectedImage(final int page) {
        return getBufferedImageOrNull(expectedImages.get(page));
    }

    public BufferedImage getActualImage(final int page) {
        return getBufferedImageOrNull(actualImages.get(page));
    }

    private BufferedImage getBufferedImageOrNull(final ImageWithDimension imageWithDimension) {
        return imageWithDimension == null ? null : imageWithDimension.bufferedImage;
    }
}
