package de.redsix.pdfcompare;

import static com.google.common.collect.Maps.newTreeMap;

import java.awt.image.BufferedImage;
import java.util.Map;
/**
 * A CompareResult, that also stores the expected and actual Image and also keeps diffImages in memory for later display.
 */
public class CompareResultWithExpectedAndActual extends CompareResultImpl {

    private final Map<Integer, ImageWithDimension> expectedImages = newTreeMap();
    private final Map<Integer, ImageWithDimension> actualImages = newTreeMap();

    @Override
    public void addPage(final PageDiffCalculator diffCalculator, final int pageIndex,
            final ImageWithDimension expectedImage, final ImageWithDimension actualImage, final ImageWithDimension diffImage) {
        super.addPage(diffCalculator, pageIndex, expectedImage, actualImage, diffImage);
        expectedImages.put(pageIndex, expectedImage);
        actualImages.put(pageIndex, actualImage);
    }

    @Override
    protected boolean keepImages() {
        return true;
    }

    public synchronized BufferedImage getDiffImage(final int page) {
        return getBufferedImageOrNull(diffImages.get(page));
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
