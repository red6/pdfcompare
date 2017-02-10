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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

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
public class CompareResult {

    private Map<Integer, BufferedImage> diffImages = new TreeMap<>();
    private boolean isEqual = true;
    private Collection<Integer> diffPages = new TreeSet<>();

    /**
     * Write the result to a file.
     *
     * @param filename without pdf-Extension
     * @return a boolean indicating, whether the comparison is equal. When true, the files are equal.
     */
    public boolean writeTo(String filename) {
        try (PDDocument document = new PDDocument()) {
            for (Entry<Integer, BufferedImage> entry : diffImages.entrySet()) {
                final BufferedImage image = entry.getValue();
                PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
                document.addPage(page);
                final PDImageXObject imageXObject = LosslessFactory.createFromImage(document, image);
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.drawImage(imageXObject, 0, 0);
                }
            }
            document.save(filename + ".pdf");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return isEqual;
    }

    public void addPage(final boolean hasDifferences, final int pageIndex,
            final BufferedImage expectedImage, final BufferedImage actualImage, final BufferedImage diffImage) {
        if (hasDifferences) {
            isEqual = false;
            diffPages.add(pageIndex);
        }
        diffImages.put(pageIndex, diffImage);
    }

    public boolean isEqual() {
        return isEqual;
    }

    public boolean isNotEqual() {
        return !isEqual;
    }

    public BufferedImage getDiffImage(final int page) {
        return diffImages.get(page);
    }

    public int getNumberOfPages() {
        if (diffImages.isEmpty()) {
            return 0;
        }
        return Collections.max(diffImages.keySet());
    }

    public Collection<Integer> getPagesThatDiffer() {
        return diffPages;
    }
}
