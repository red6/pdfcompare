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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class PdfComparator<T extends CompareResult> {

    private static final int DPI = 300;
    private static final int EXTRA_RGB = new Color(0, 160, 0).getRGB();
    private static final int MISSING_RGB = new Color(220, 0, 0).getRGB();
    public static final int MARKER_WIDTH = 20;
    private final Exclusions exclusions = new Exclusions();
    private final T compareResult;

    public PdfComparator() {
        this.compareResult = (T) new CompareResult();
    }

    public PdfComparator(T compareResult) {
        this.compareResult = compareResult;
    }

    public T compare(String expectedPdfFilename, String actualPdfFilename) throws IOException {
        Objects.requireNonNull(expectedPdfFilename, "expectedPdfFilename is null");
        Objects.requireNonNull(actualPdfFilename, "actualPdfFilename is null");
        return compare(new File(expectedPdfFilename), new File(actualPdfFilename));
    }

    public T compare(String expectedPdfFilename, String actualPdfFilename, String ignoreFilename) throws IOException {
        Objects.requireNonNull(ignoreFilename, "ignoreFilename is null");
        exclusions.readExclusions(ignoreFilename);
        return compare(expectedPdfFilename, actualPdfFilename);
    }

    public T compare(final Path expectedPath, final Path actualPath) throws IOException {
        Objects.requireNonNull(expectedPath, "expectedPath is null");
        Objects.requireNonNull(actualPath, "actualPath is null");
        if (expectedPath.equals(actualPath)) {
            return compareResult;
        }
        try (final InputStream expectedPdfIS = Files.newInputStream(expectedPath)) {
            try (final InputStream actualPdfIS = Files.newInputStream(actualPath)) {
                return compare(expectedPdfIS, actualPdfIS);
            } catch (NoSuchFileException ex) {
                addSingleDocumentToResult(expectedPdfIS, MISSING_RGB);
                return compareResult;
            }
        } catch (NoSuchFileException ex) {
            if (Files.exists(actualPath)) {
                try (final InputStream actualPdfIS = Files.newInputStream(actualPath)) {
                    addSingleDocumentToResult(actualPdfIS, EXTRA_RGB);
                }
            }
            return compareResult;
        }
    }

    public T compare(final Path expectedPath, final Path actualPath, final Path ignorePath) throws IOException {
        Objects.requireNonNull(ignorePath, "ignorePath is null");
        exclusions.readExclusions(ignorePath);
        return compare(expectedPath, actualPath);
    }

    public T compare(final File expectedFile, final File actualFile) throws IOException {
        Objects.requireNonNull(expectedFile, "expectedFile is null");
        Objects.requireNonNull(actualFile, "actualFile is null");
        if (expectedFile.equals(actualFile)) {
            return compareResult;
        }
        try (final InputStream expectedPdfIS = new FileInputStream(expectedFile)) {
            try (final InputStream actualPdfIS = new FileInputStream(actualFile)) {
                return compare(expectedPdfIS, actualPdfIS);
            } catch (NoSuchFileException ex) {
                addSingleDocumentToResult(expectedPdfIS, MISSING_RGB);
                return compareResult;
            }
        } catch (NoSuchFileException ex) {
            if (actualFile.exists()) {
                try (final InputStream actualPdfIS = new FileInputStream(actualFile)) {
                    addSingleDocumentToResult(actualPdfIS, EXTRA_RGB);
                }
            }
            return compareResult;
        }
    }

    public T compare(final File expectedFile, final File actualFile, final File ignoreFile) throws IOException {
        Objects.requireNonNull(ignoreFile, "ignoreFile is null");
        exclusions.readExclusions(ignoreFile);
        return compare(expectedFile, actualFile);
    }

    public T compare(InputStream expectedPdfIS, InputStream actualPdfIS, InputStream ignoreIS) throws IOException {
        Objects.requireNonNull(ignoreIS, "ignoreIS is null");
        exclusions.readExclusions(ignoreIS);
        return compare(expectedPdfIS, actualPdfIS);
    }

    private void addSingleDocumentToResult(InputStream expectedPdfIS, int markerColor) throws IOException {
        try (PDDocument expectedDocument = PDDocument.load(expectedPdfIS)) {
            PDFRenderer expectedPdfRenderer = new PDFRenderer(expectedDocument);
            addExtraPages(expectedDocument, expectedPdfRenderer, 0, markerColor, true);
        }
    }

    public T compare(InputStream expectedPdfIS, InputStream actualPdfIS) throws IOException {
        Objects.requireNonNull(expectedPdfIS, "expectedPdfIS is null");
        Objects.requireNonNull(actualPdfIS, "actualPdfIS is null");
        if (expectedPdfIS.equals(actualPdfIS)) {
            return compareResult;
        }
        try (PDDocument expectedDocument = PDDocument.load(expectedPdfIS)) {
            PDFRenderer expectedPdfRenderer = new PDFRenderer(expectedDocument);
            try (PDDocument actualDocument = PDDocument.load(actualPdfIS)) {
                PDFRenderer actualPdfRenderer = new PDFRenderer(actualDocument);
                final int minPageCount = Math.min(expectedDocument.getNumberOfPages(), actualDocument.getNumberOfPages());
                for (int pageIndex = 0; pageIndex < minPageCount; pageIndex++) {
                    BufferedImage expectedImage = renderPageAsImage(expectedPdfRenderer, pageIndex);
                    BufferedImage actualImage = renderPageAsImage(actualPdfRenderer, pageIndex);
                    compare(expectedImage, actualImage, pageIndex);
                }
                if (expectedDocument.getNumberOfPages() > minPageCount) {
                    addExtraPages(expectedDocument, expectedPdfRenderer, minPageCount, MISSING_RGB, true);
                } else if (actualDocument.getNumberOfPages() > minPageCount) {
                    addExtraPages(actualDocument, actualPdfRenderer, minPageCount, EXTRA_RGB, false);
                }
            }
        }
        return compareResult;
    }

    private void addExtraPages(final PDDocument document, final PDFRenderer pdfRenderer, final int minPageCount,
            final int color, final boolean expected) throws IOException {
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
            if (expected) {
                compareResult.addPage(true, pageIndex, image, blank(image), image);
            } else {
                compareResult.addPage(true, pageIndex, blank(image), image, image);
            }
        }
    }

    private static BufferedImage blank(final BufferedImage image) {
        return new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
    }

    private BufferedImage renderPageAsImage(final PDFRenderer expectedPdfRenderer, final int pageIndex) throws IOException {
        return expectedPdfRenderer.renderImageWithDPI(pageIndex, DPI);
    }

    private void compare(final BufferedImage expectedImage, final BufferedImage actualImage, final int pageIndex) {
        final DiffImage diffImage = new DiffImage(expectedImage, actualImage, pageIndex, exclusions);
        compareResult.addPage(diffImage.differs(), pageIndex, expectedImage, actualImage, diffImage.getImage());
    }
}
