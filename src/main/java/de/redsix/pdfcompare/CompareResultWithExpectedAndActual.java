package de.redsix.pdfcompare;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.TreeMap;

/**
 * A CompareResult, that also stores the expected and actual Image for later display.
 */
public class CompareResultWithExpectedAndActual extends CompareResult {

    private Map<Integer, BufferedImage> expectedImages = new TreeMap<>();
    private Map<Integer, BufferedImage> actualImages = new TreeMap<>();

    public void addPageThatsEqual(final int pageIndex, final BufferedImage diffImage) {
        super.addPageThatsEqual(pageIndex, diffImage);
        expectedImages.put(pageIndex, diffImage);
        actualImages.put(pageIndex, diffImage);
    }

    public void addPageThatsNotEqual(final int pageIndex, final BufferedImage expectedImage, final BufferedImage actualImage, final BufferedImage diffImage) {
        super.addPageThatsNotEqual(pageIndex, expectedImage, actualImage, diffImage);
        expectedImages.put(pageIndex, expectedImage);
        actualImages.put(pageIndex, actualImage);
    }

    public BufferedImage getExpectedImage(final int page) {
        return expectedImages.get(page);
    }

    public BufferedImage getActualImage(final int page) {
        return actualImages.get(page);
    }
}
