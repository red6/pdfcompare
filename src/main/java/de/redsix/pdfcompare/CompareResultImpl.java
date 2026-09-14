/*
 * Copyright 2016 Malte Finsterwalder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.redsix.pdfcompare;

import de.redsix.pdfcompare.env.Environment;

import java.awt.Color;
import java.awt.image.BufferedImage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * A CompareResult tracks the differences, that result from a comparison.
 * The CompareResult only stores the diffImages, for lower memory consumption.
 * If you also need the expected and actual Image, please use the Subclass
 * {@link CompareResultWithExpectedAndActual}
 */
public class CompareResultImpl implements ResultCollector, CompareResult {

    private static final Logger LOG = LoggerFactory.getLogger(CompareResultImpl.class);
    /** Alpha value for the semi-transparent diff overlay in horizontal compare mode (0 = transparent, 1 = opaque). */
    private static final float OVERLAY_ALPHA = 0.45f;
    /** Default pixel color (white) used for out-of-bounds areas when images differ in size. */
    private static final int WHITE_PIXEL = 0xFFFFFF;
    protected Environment environment;
    protected final Map<Integer, ImageWithDimension> diffImages = new TreeMap<>();
    /** Stores the actual-overlay images per page when horizontal compare output is enabled. */
    protected final Map<Integer, ImageWithDimension> diffImagesActualOverlay = new TreeMap<>();
    protected boolean isEqual = true;
    protected boolean hasDifferenceInExclusion = false;
    private boolean expectedOnly;
    private boolean actualOnly;
    private final Collection<PageArea> diffAreas = new ArrayList<>();
    private final Map<Integer, Double> diffPercentages = new TreeMap<>();
    private int pages = 0;

    @Override
    public boolean writeTo(String filename) {
        return writeTo(doc -> doc.save(filename + ".pdf"));
    }

    @Override
    public boolean writeTo(final OutputStream outputStream) {
        Objects.requireNonNull(outputStream, "OutputStream must not be null");
        final boolean result = writeTo(doc -> doc.save(outputStream));
        silentlyCloseOutputStream(outputStream);
        return result;
    }

    private boolean writeTo(ThrowingConsumer<PDDocument, IOException> saver) {
        if (hasImages()) {
            try (PDDocument document = new PDDocument()) {
                addImagesToDocument(document);
                saver.accept(document);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return isEqual;
    }

    private void silentlyCloseOutputStream(final OutputStream outputStream) {
        try {
            outputStream.close();
        } catch (IOException e) {
            LOG.info("Could not close OutputStream", e);
        }
    }

    /**
     * checks, whether this CompareResult has stored images.
     *
     * @return true, when images are stored in this CompareResult
     */
    protected synchronized boolean hasImages() {
        return !diffImages.isEmpty();
    }

    protected synchronized void addImagesToDocument(final PDDocument document) throws IOException {
        if (environment != null && environment.getEnableHorizontalCompareOutput()) {
            final Iterator<Entry<Integer, ImageWithDimension>> iterator = diffImages.entrySet().iterator();
            while (iterator.hasNext()) {
                final Entry<Integer, ImageWithDimension> entry = iterator.next();
                final int key = entry.getKey();
                if (!keepImages()) {
                    iterator.remove();
                }
                // Page A: Expected with red overlay
                addPageToDocument(document, entry.getValue());
                // Page B: Actual with green overlay
                final ImageWithDimension actualOverlay = diffImagesActualOverlay.get(key);
                if (actualOverlay != null) {
                    if (!keepImages()) {
                        diffImagesActualOverlay.remove(key);
                    }
                    addPageToDocument(document, actualOverlay);
                }
            }
        } else {
            addImagesToDocument(document, diffImages);
        }
    }

    protected synchronized void addImagesToDocument(final PDDocument document, final Map<Integer, ImageWithDimension> images)
            throws IOException {
        final Iterator<Entry<Integer, ImageWithDimension>> iterator = images.entrySet().iterator();
        while (iterator.hasNext()) {
            final Entry<Integer, ImageWithDimension> entry = iterator.next();
            if (!keepImages()) {
                iterator.remove();
            }
            addPageToDocument(document, entry.getValue());
        }
    }

    protected void addPageToDocument(final PDDocument document, final ImageWithDimension image) throws IOException {
        PDPage page = new PDPage(new PDRectangle(image.width, image.height));
        document.addPage(page);
        final PDImageXObject imageXObject = LosslessFactory.createFromImage(document, image.bufferedImage);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.drawImage(imageXObject, 0, 0, image.width, image.height);
        }
    }

    protected boolean keepImages() {
        return false;
    }

    @Override
    public synchronized void addPage(final PageDiffCalculator diffCalculator, final int pageIndex,
            final ImageWithDimension expectedImage, final ImageWithDimension actualImage, final ImageWithDimension diffImage) {
        Objects.requireNonNull(expectedImage, "expectedImage is null");
        Objects.requireNonNull(actualImage, "actualImage is null");
        Objects.requireNonNull(diffImage, "diffImage is null");

        this.hasDifferenceInExclusion |= diffCalculator.differencesFoundInExclusion();
        diffPercentages.put(pageIndex, diffCalculator.getDifferenceInPercent());

        if (this.environment.getEnableHorizontalCompareOutput()) {
            // Two pages per logical page: readable background with semi-transparent color overlay on differing pixels
            // Page A: expected image as background + semi-transparent red overlay on differing pixels
            final ImageWithDimension expectedOverlay = createOverlay(
                    expectedImage, actualImage, environment.getActualColor(), OVERLAY_ALPHA, true);
            // Page B: actual image as background + semi-transparent green overlay on differing pixels
            final ImageWithDimension actualOverlay = createOverlay(
                    expectedImage, actualImage, environment.getExpectedColor(), OVERLAY_ALPHA, false);
            storeImagesForPageHorizontal(diffCalculator, pageIndex, expectedOverlay, actualOverlay);
        } else {
            storeImageForPage(diffCalculator, pageIndex, diffImage);
        }
    }

    /**
     * Creates an overlay image: the background image (expected or actual) remains fully readable at 100% opacity.
     * Pixels that differ between expected and actual are blended with the given overlay color
     * at the specified alpha level (0 = transparent, 1 = opaque).
     *
     * @param expected              the expected image
     * @param actual                the actual image
     * @param overlayColor          color of the overlay for differing pixels
     * @param alpha                 opacity of the overlay (0 = transparent, 1 = opaque)
     * @param useExpectedAsBackground if true, uses expected as background; otherwise uses actual
     */
    private ImageWithDimension createOverlay(final ImageWithDimension expected, final ImageWithDimension actual,
            final Color overlayColor, final float alpha, final boolean useExpectedAsBackground) {
        final BufferedImage expectedBuf = expected.bufferedImage;
        final BufferedImage actualBuf = actual.bufferedImage;
        final BufferedImage backgroundBuf = useExpectedAsBackground ? expectedBuf : actualBuf;

        final int width  = Math.max(expectedBuf.getWidth(),  actualBuf.getWidth());
        final int height = Math.max(expectedBuf.getHeight(), actualBuf.getHeight());

        final BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        final float backgroundAlpha = 1.0f - alpha;
        final int overlayRed   = overlayColor.getRed();
        final int overlayGreen = overlayColor.getGreen();
        final int overlayBlue  = overlayColor.getBlue();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int expectedElement = (x < expectedBuf.getWidth() && y < expectedBuf.getHeight())
                        ? expectedBuf.getRGB(x, y) : WHITE_PIXEL;
                final int actualElement = (x < actualBuf.getWidth()   && y < actualBuf.getHeight())
                        ? actualBuf.getRGB(x, y) : WHITE_PIXEL;
                final int backgroundElement  = (x < backgroundBuf.getWidth() && y < backgroundBuf.getHeight())
                        ? backgroundBuf.getRGB(x, y) : WHITE_PIXEL;

                final int resultRgb;
                if (expectedElement != actualElement) {
                    // Difference found: blend overlay color semi-transparently onto background
                    final Color bg = new Color(backgroundElement);
                    final int r = Math.min(255, (int)(bg.getRed()   * backgroundAlpha + overlayRed   * alpha));
                    final int g = Math.min(255, (int)(bg.getGreen() * backgroundAlpha + overlayGreen * alpha));
                    final int b = Math.min(255, (int)(bg.getBlue()  * backgroundAlpha + overlayBlue  * alpha));
                    resultRgb = new Color(r, g, b).getRGB();
                } else {
                    // No difference: copy background pixel unchanged
                    resultRgb = backgroundElement;
                }
                result.setRGB(x, y, resultRgb);
            }
        }

        final ImageWithDimension bgImage = useExpectedAsBackground ? expected : actual;
        return new ImageWithDimension(result, bgImage.width, bgImage.height);
    }

    /**
     * Stores the expected and actual overlay images for the horizontal compare mode.
     */
    private void storeImagesForPageHorizontal(final PageDiffCalculator diffCalculator, final int pageIndex,
            final ImageWithDimension expectedOverlay, final ImageWithDimension actualOverlay) {
        if (diffCalculator.differencesFound()) {
            isEqual = false;
            diffAreas.add(diffCalculator.getDiffArea());
            diffImages.put(pageIndex, expectedOverlay);
            diffImagesActualOverlay.put(pageIndex, actualOverlay);
            pages++;
        } else if (environment.addEqualPagesToResult()) {
            diffImages.put(pageIndex, expectedOverlay);
            diffImagesActualOverlay.put(pageIndex, actualOverlay);
            pages++;
        }
    }

    /**
     * Stores the diff image for the standard mode (without horizontal compare).
     */
    private void storeImageForPage(final PageDiffCalculator diffCalculator, final int pageIndex,
            final ImageWithDimension image) {
        if (diffCalculator.differencesFound()) {
            isEqual = false;
            diffAreas.add(diffCalculator.getDiffArea());
            diffImages.put(pageIndex, image);
            pages++;
        } else if (environment.addEqualPagesToResult()) {
            diffImages.put(pageIndex, image);
            pages++;
        }
    }

    @Override
    public void noPagesFound() {
        isEqual = false;
    }

    @Override
    public boolean isEqual() {
        return isEqual;
    }

    @Override
    public boolean isNotEqual() {
        return !isEqual;
    }

    @Override
    public boolean hasDifferenceInExclusion() {
        return hasDifferenceInExclusion;
    }

    @Override
    public boolean hasOnlyExpected() {
        return expectedOnly;
    }

    @Override
    public boolean hasOnlyActual() {
        return actualOnly;
    }

    @Override
    public boolean hasOnlyOneDoc() {
        return expectedOnly || actualOnly;
    }

    @Override
    public int getNumberOfPages() {
        return pages;
    }

    @Override
    public Collection<PageArea> getDifferences() {
        return diffAreas;
    }

    @Override
    public String getDifferencesJson() {
        return PageArea.asJsonWithExclusion(getDifferences());
    }

    @Override
    public Collection<Integer> getPagesWithDifferences() {
        return diffAreas.stream().map(a -> a.page).collect(Collectors.toList());
    }

    @Override
    public Map<Integer, Double> getPageDiffsInPercent() {
        return diffPercentages;
    }

    public void expectedOnly() {
        this.expectedOnly = true;
    }

    public void actualOnly() {
        this.actualOnly = true;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

}
