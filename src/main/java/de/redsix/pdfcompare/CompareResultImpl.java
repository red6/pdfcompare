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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.redsix.pdfcompare.env.Environment;

/**
 * A CompareResult tracks the differences, that result from a comparison.
 * The CompareResult only stores the diffImages, for lower memory consumption.
 * If you also need the expected and actual Image, please use the Subclass
 * {@link CompareResultWithExpectedAndActual}
 */
public class CompareResultImpl implements ResultCollector, CompareResult {

    private static final Logger LOG = LoggerFactory.getLogger(CompareResultImpl.class);
    protected Environment environment;
    protected final Map<Integer, ImageWithDimension> diffImages = new TreeMap<>();
    protected boolean isEqual = true;
    protected boolean hasDifferenceInExclusion = false;
    private boolean expectedOnly;
    private boolean actualOnly;
    private Collection<PageArea> diffAreas = new ArrayList<>();
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
     * @return true, when images are stored in this CompareResult
     */
    protected synchronized boolean hasImages() {
        return !diffImages.isEmpty();
    }

    protected synchronized void addImagesToDocument(final PDDocument document) throws IOException {
        addImagesToDocument(document, diffImages);
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
        if (diffCalculator.differencesFound()) {
            isEqual = false;
            diffAreas.add(diffCalculator.getDiffArea());
        }
        diffImages.put(pageIndex, diffImage);
        pages++;
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
