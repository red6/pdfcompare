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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class PdfComparator {

    private static final int DPI = 300;
    private static final int MARKER_RGB = Color.MAGENTA.getRGB();
    private static final int EXTRA_RGB = Color.GREEN.getRGB();
    private static final int MISSING_RGB = Color.RED.getRGB();
    private static final int MARKER_WIDTH = 20;

    public CompareResult compare(String expectedPdfFilename, String actualPdfFilename) throws IOException {
        if (expectedPdfFilename.equals(actualPdfFilename)) {
            return new CompareResult();
        }
        return compare(new FileInputStream(expectedPdfFilename), new FileInputStream(actualPdfFilename));
    }

    public CompareResult compare(InputStream expectedPdfIS, InputStream actualPdfIS) throws IOException {
        final CompareResult result = new CompareResult();
        if (expectedPdfIS.equals(actualPdfIS)) {
            return result;
        }
        final InputStream expectedPdf = expectedPdfIS;
        final InputStream actualPdf = actualPdfIS;
//        final InputStream actualPdf = new BufferedInputStream(actualPdfIS);
//        final InputStream expectedPdf = new BufferedInputStream(expectedPdfIS);
//        if (expectedPdf.markSupported() && actualPdf.markSupported()) {
//            expectedPdf.mark(expectedPdf.available());
//            actualPdf.mark(expectedPdf.available());
//            if (isBinaryEquals(expectedPdf, actualPdf)) {
//                return result;
//            }
//            expectedPdf.reset();
//            actualPdf.reset();
//        }
        try (PDDocument expectedDocument = PDDocument.load(expectedPdf)) {
            PDFRenderer expectedPdfRenderer = new PDFRenderer(expectedDocument);
            try (PDDocument actualDocument = PDDocument.load(actualPdf)) {
                PDFRenderer actualPdfRenderer = new PDFRenderer(actualDocument);
                final int minPageCount = Math.min(expectedDocument.getNumberOfPages(), actualDocument.getNumberOfPages());
                for (int pageIndex = 0; pageIndex < minPageCount; pageIndex++) {
                    BufferedImage expectedImage = renderPageAsImage(expectedPdfRenderer, pageIndex);
                    BufferedImage actualImage = renderPageAsImage(actualPdfRenderer, pageIndex);
                    compare(expectedImage, actualImage, pageIndex, result);
                }
                if (expectedDocument.getNumberOfPages() > minPageCount) {
                    addExtraPages(expectedDocument, expectedPdfRenderer, minPageCount, result, MISSING_RGB);
                } else if (actualDocument.getNumberOfPages() > minPageCount) {
                    addExtraPages(actualDocument, actualPdfRenderer, minPageCount, result, EXTRA_RGB);
                }
            }
        }
        return result;
    }

    private boolean isBinaryEquals(final InputStream expectedPdf, final InputStream actualPdf) throws IOException {
        int expectedRead;
        while((expectedRead = expectedPdf.read()) != -1) {
            if (expectedRead != actualPdf.read()) {
                return false;
            }
        }
        return true;
    }

    private void addExtraPages(final PDDocument document, final PDFRenderer pdfRenderer, final int minPageCount, final CompareResult result,
            final int color) throws IOException {
        for (int pageIndex = minPageCount; pageIndex < document.getNumberOfPages(); pageIndex++) {
            BufferedImage image = renderPageAsImage(pdfRenderer, pageIndex);
            final DataBuffer dataBuffer = image.getRaster().getDataBuffer();
            for (int i = 0; i < image.getWidth() * MARKER_WIDTH; i++) {
                dataBuffer.setElem(i, color);
            }
            for (int i = 0; i < image.getHeight(); i++) {
                for (int j = 0; j < MARKER_WIDTH; j++) {
                    dataBuffer.setElem(i * image.getWidth() + j, color);
                }
            }
            result.addPageThatsNotEqual(pageIndex, image);
        }
    }

    private BufferedImage renderPageAsImage(final PDFRenderer expectedPdfRenderer, final int pageIndex) throws IOException {
        return expectedPdfRenderer.renderImageWithDPI(pageIndex, DPI);
    }

    private void compare(final BufferedImage expectedImage, final BufferedImage actualImage, final int pageIndex,
            final CompareResult result) {
        Optional<BufferedImage> diffImage = diffImages(expectedImage, actualImage);
        if (diffImage.isPresent()) {
            result.addPageThatsNotEqual(pageIndex, diffImage.get());
        } else {
            result.addPageThatsEqual(pageIndex, expectedImage);
        }
    }

    /**
     * Modifies the actualImage marking the differences, when they are found
     */
    private Optional<BufferedImage> diffImages(final BufferedImage expectedImage, final BufferedImage actualImage) {
        final WritableRaster expectedImageRaster = expectedImage.getRaster();
        final DataBuffer expectedBuffer = expectedImageRaster.getDataBuffer();
        final WritableRaster actualImageRaster = actualImage.getRaster();
        final DataBuffer actualBuffer = actualImageRaster.getDataBuffer();

        final int expectedImageWidth = expectedImage.getWidth();
        final int actualImageWidth = actualImage.getWidth();

        int[] diffPixel;

        boolean diffFound = false;
        final int maxIndex = Math.min(expectedBuffer.getSize(), actualBuffer.getSize());
        for (int i = 0; i < maxIndex; i++) {
            if (expectedBuffer.getElem(i) != actualBuffer.getElem(i)) {
                diffFound = true;
                final int[] expectedPixel = expectedImageRaster.getPixel(i % expectedImageWidth, i / expectedImageWidth, (int[]) null);
                final int[] actualPixel = actualImageRaster.getPixel(i % actualImageWidth, i / actualImageWidth, (int[]) null);
                int expectedDarkness = calcDarkness(expectedPixel);
                int actualDarkness = calcDarkness(actualPixel);
                if (expectedDarkness > actualDarkness) {
                    diffPixel = new int[] {Math.max(180, Math.min(expectedDarkness / 3, 255)), 0, 0};
                } else {
                    diffPixel = new int[] {0, Math.max(180, Math.min(actualDarkness / 3, 255)), 0};
                }
                actualImageRaster.setPixel(i % actualImageWidth, i / actualImageWidth, diffPixel);
                mark(actualBuffer, i, actualImageWidth, MARKER_RGB);
            }
        }
        if (actualBuffer.getSize() != expectedBuffer.getSize()) {
            if (actualBuffer.getSize() > expectedBuffer.getSize()) {
                for (int i = expectedBuffer.getSize() + 1; i < actualBuffer.getSize(); i++) {
                    mark(actualBuffer, i, actualImageWidth, MARKER_RGB);
                }
            } else {
                mark(actualBuffer, actualBuffer.getSize() - 1, actualImageWidth, MARKER_RGB);
                diffFound = true;
            }
        }
        if (diffFound) {
            return Optional.of(actualImage);
        }
        return Optional.empty();
    }

    private int calcDarkness(final int[] pixel) {
        int result = 0;
        for (int i : pixel) {
            result += i;
        }
        return result;
    }

    private void mark(final DataBuffer expectedImageBuffer, final int index, final int imageWidth, final int markerRGB) {
        int column = index % imageWidth;
        for (int i = 0; i < MARKER_WIDTH; i++) {
            expectedImageBuffer.setElem(column + i * imageWidth, markerRGB);
            expectedImageBuffer.setElem(index - column + i, markerRGB);
        }
    }
}
