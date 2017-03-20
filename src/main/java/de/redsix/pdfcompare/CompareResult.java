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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
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

/**
 * A CompareResult tracks the differences, that result from a comparison.
 * The CompareResult only stores the diffImages, for lower memory consumption.
 * If you also need the expected and actual Image, please use the Subclass
 * {@link CompareResultWithExpectedAndActual}
 */
public class CompareResult implements ResultCollector {

    protected final Map<Integer, BufferedImage> diffImages = new TreeMap<>();
    protected boolean isEqual = true;
    protected boolean hasDifferenceInExclusion = false;

    /**
     * Write the result Pdf to a file. Warning: This will remove the diffImages from memory!
     * Writing can only be done once.
     *
     * @param filename without pdf-Extension
     * @return a boolean indicating, whether the comparison is equal. When true, the files are equal.
     */
    public synchronized boolean writeTo(String filename) {
        if (!hasImages()) {
            return isEqual;
        }
        try (PDDocument document = new PDDocument()) {
            addImagesToDocument(document);
            document.save(filename + ".pdf");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return isEqual;
    }

    protected boolean hasImages() {
        return !diffImages.isEmpty();
    }

    protected void addImagesToDocument(final PDDocument document) throws IOException {
        final Iterator<Entry<Integer, BufferedImage>> iterator = diffImages.entrySet().iterator();
        while (iterator.hasNext()) {
            final Entry<Integer, BufferedImage> entry = iterator.next();
            if (!keepImages()) {
                iterator.remove();
            }
            addPageToDocument(document, entry.getValue());
        }
    }

    protected void addPageToDocument(final PDDocument document, final BufferedImage image) throws IOException {
        PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
        document.addPage(page);
        final PDImageXObject imageXObject = LosslessFactory.createFromImage(document, image);
        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.drawImage(imageXObject, 0, 0);
        }
    }

    protected boolean keepImages() {
        return false;
    }

    @Override
    public synchronized void addPage(final boolean hasDifferences, final boolean hasDifferenceInExclusion, final int pageIndex,
            final BufferedImage expectedImage, final BufferedImage actualImage, final BufferedImage diffImage) {
        Objects.requireNonNull(expectedImage, "expectedImage is null");
        Objects.requireNonNull(actualImage, "actualImage is null");
        Objects.requireNonNull(diffImage, "diffImage is null");
        this.hasDifferenceInExclusion |= hasDifferenceInExclusion;
        if (hasDifferences) {
            isEqual = false;
        }
        diffImages.put(pageIndex, diffImage);
    }

    @Override
    public void noPagesFound() {
        isEqual = false;
    }

    public boolean isEqual() {
        return isEqual;
    }

    public boolean isNotEqual() {
        return !isEqual;
    }

    public boolean hasDifferenceInExclusion() {
        return hasDifferenceInExclusion;
    }

    public synchronized int getNumberOfPages() {
        if (!hasImages()) {
            return 0;
        }
        return Collections.max(diffImages.keySet());
    }
}
