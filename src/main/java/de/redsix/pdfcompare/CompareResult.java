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
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public class CompareResult {

    private Map<Integer, BufferedImage> diffImages = new TreeMap<>();
    private Map<Integer, BufferedImage> expectedImages = new TreeMap<>();
    private Map<Integer, BufferedImage> actualImages = new TreeMap<>();
    private boolean isEqual = true;

    /**
     * Either write a file or do nothing, when there are no differences.
     *
     * @param filename without pdf-Extension
     * @return a boolean indicating, whether the files are equal. When true, the files are equal and nothing was saved.
     */
    public boolean writeTo(String filename) {
        if (isEqual) {
            return isEqual;
        }
        try (PDDocument document = new PDDocument()) {
            for (Entry<Integer, BufferedImage> entry : diffImages.entrySet()) {
                final BufferedImage image = entry.getValue();
                PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
                document.addPage(page);
                final String tmpFilename = filename + "_page_" + (entry.getKey() + 1) + ".png";
                final File tmpFile = new File(tmpFilename);
                final PDImageXObject imageXObject = LosslessFactory.createFromImage(document, image);
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.drawImage(imageXObject, 0, 0);
                }
                tmpFile.delete();
            }
            document.save(filename + ".pdf");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return isEqual;
    }

    public void addPageThatsEqual(final int pageIndex, final BufferedImage diffImage) {
        expectedImages.put(pageIndex, diffImage);
        actualImages.put(pageIndex, diffImage);
        diffImages.put(pageIndex, diffImage);
    }

    public void addPageThatsNotEqual(final int pageIndex, final BufferedImage diffImage) {
        isEqual = false;
        diffImages.put(pageIndex, diffImage);
    }

    public void addPageThatsNotEqual(final int pageIndex, final BufferedImage expectedImage, final BufferedImage actualImage, final BufferedImage diffImage) {
        isEqual = false;
        expectedImages.put(pageIndex, expectedImage);
        actualImages.put(pageIndex, actualImage);
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

    public BufferedImage getExpectedImage(final int page) {
        return expectedImages.get(page);
    }

    public BufferedImage getActualImage(final int page) {
        return actualImages.get(page);
    }

    public int getNumberOfPages() {
        if (diffImages.isEmpty()) {
            return 0;
        }
        return Collections.max(diffImages.keySet());
    }
}
