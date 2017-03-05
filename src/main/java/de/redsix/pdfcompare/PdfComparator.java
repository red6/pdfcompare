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
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfComparator<T extends CompareResult> {

    private static final Logger LOG = LoggerFactory.getLogger(PdfComparator.class);
    private static final int DPI = 300;
    private static final int EXTRA_RGB = new Color(0, 160, 0).getRGB();
    private static final int MISSING_RGB = new Color(220, 0, 0).getRGB();
    public static final int MARKER_WIDTH = 20;
    private final Exclusions exclusions = new Exclusions();
    private InputStreamSupplier expectedStreamSupplier;
    private InputStreamSupplier actualStreamSupplier;
    private Executor executor;
    private final T compareResult;

    private PdfComparator(T compareResult) {
        Objects.requireNonNull(compareResult, "compareResult is null");
        this.compareResult = compareResult;
    }

    public PdfComparator(String expectedPdfFilename, String actualPdfFilename) throws IOException {
        this(expectedPdfFilename, actualPdfFilename, (T) new CompareResult());
    }

    public PdfComparator(String expectedPdfFilename, String actualPdfFilename, T compareResult) throws IOException {
        this(compareResult);
        Objects.requireNonNull(expectedPdfFilename, "expectedPdfFilename is null");
        Objects.requireNonNull(actualPdfFilename, "actualPdfFilename is null");
        if (!expectedPdfFilename.equals(actualPdfFilename)) {
            this.expectedStreamSupplier = () -> Files.newInputStream(Paths.get(expectedPdfFilename));
            this.actualStreamSupplier = () -> Files.newInputStream(Paths.get(actualPdfFilename));
        }
    }

    public PdfComparator(final Path expectedPath, final Path actualPath) throws IOException {
        this(expectedPath, actualPath, (T) new CompareResult());
    }

    public PdfComparator(final Path expectedPath, final Path actualPath, final T compareResult) throws IOException {
        this(compareResult);
        Objects.requireNonNull(expectedPath, "expectedPath is null");
        Objects.requireNonNull(actualPath, "actualPath is null");
        if (!expectedPath.equals(actualPath)) {
            this.expectedStreamSupplier = () -> Files.newInputStream(expectedPath);
            this.actualStreamSupplier = () -> Files.newInputStream(actualPath);
        }
    }

    public PdfComparator(final File expectedFile, final File actualFile) throws IOException {
        this(expectedFile, actualFile, (T) new CompareResult());
    }

    public PdfComparator(final File expectedFile, final File actualFile, final T compareResult) throws IOException {
        this(compareResult);
        Objects.requireNonNull(expectedFile, "expectedFile is null");
        Objects.requireNonNull(actualFile, "actualFile is null");
        if (!expectedFile.equals(actualFile)) {
            this.expectedStreamSupplier = () -> new FileInputStream(expectedFile);
            this.actualStreamSupplier = () -> new FileInputStream(actualFile);
        }
    }

    public PdfComparator(final InputStream expectedPdfIS, final InputStream actualPdfIS) {
        this(expectedPdfIS, actualPdfIS, (T) new CompareResult());
    }

    public PdfComparator(final InputStream expectedPdfIS, final InputStream actualPdfIS, final T compareResult) {
        this(compareResult);
        Objects.requireNonNull(expectedPdfIS, "expectedPdfIS is null");
        Objects.requireNonNull(actualPdfIS, "actualPdfIS is null");
        if (!expectedPdfIS.equals(actualPdfIS)) {
            this.expectedStreamSupplier = () -> expectedPdfIS;
            this.actualStreamSupplier = () -> actualPdfIS;
        }
    }

    public PdfComparator<T> withIgnore(final String ignoreFilename) {
        Objects.requireNonNull(ignoreFilename, "ignoreFilename is null");
        exclusions.readExclusions(ignoreFilename);
        return this;
    }

    public PdfComparator<T> withIgnore(final File ignoreFile) {
        Objects.requireNonNull(ignoreFile, "ignoreFile is null");
        exclusions.readExclusions(ignoreFile);
        return this;
    }

    public PdfComparator<T> withIgnore(final Path ignorePath) {
        Objects.requireNonNull(ignorePath, "ignorePath is null");
        exclusions.readExclusions(ignorePath);
        return this;
    }

    public PdfComparator<T> withIgnore(InputStream ignoreIS) {
        Objects.requireNonNull(ignoreIS, "ignoreIS is null");
        exclusions.readExclusions(ignoreIS);
        return this;
    }

    public PdfComparator<T> withExecutor(final Executor executor) {
        this.executor = executor;
        return this;
    }

    public T compare() throws IOException {
        if (expectedStreamSupplier == null && actualStreamSupplier == null) {
            return compareResult;
        }
        ExecutorService executorService = null;
        if (executor == null) {
            executorService = Executors.newWorkStealingPool();
            executor = executorService;
        }
        try (final InputStream expectedStream = expectedStreamSupplier.get()) {
            try (final InputStream actualStream = actualStreamSupplier.get()) {
                try (PDDocument expectedDocument = PDDocument.load(expectedStream)) {
                    PDFRenderer expectedPdfRenderer = new PDFRenderer(expectedDocument);
                    try (PDDocument actualDocument = PDDocument.load(actualStream)) {
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
            } catch (NoSuchFileException ex) {
                addSingleDocumentToResult(expectedStream, MISSING_RGB);
            }
        } catch (NoSuchFileException ex) {
            try (final InputStream actualStream = actualStreamSupplier.get()) {
                addSingleDocumentToResult(actualStream, EXTRA_RGB);
            } catch (NoSuchFileException innerEx) {
                LOG.warn("No files found to compare. Tried Expected: {} and Actual: {}", ex.getFile(), innerEx.getFile(), innerEx);
            }
        }
        if (executorService != null) {
            executorService.shutdown();
            executor = null;
        }
        return compareResult;
    }

    private void addSingleDocumentToResult(InputStream expectedPdfIS, int markerColor) throws IOException {
        try (PDDocument expectedDocument = PDDocument.load(expectedPdfIS)) {
            PDFRenderer expectedPdfRenderer = new PDFRenderer(expectedDocument);
            addExtraPages(expectedDocument, expectedPdfRenderer, 0, markerColor, true);
        }
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
                compareResult.addPage(true, false, pageIndex, image, blank(image), image);
            } else {
                compareResult.addPage(true, false, pageIndex, blank(image), image, image);
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
        final DiffImage diffImage = new DiffImage(expectedImage, actualImage, pageIndex, exclusions, compareResult);
        executor.execute(() -> {
            try {
                diffImage.diffImages();
            } catch (Exception e) {
                LOG.error("Exception while diffing Images", e);
            }
        });
    }

    public T getResult() {
        return compareResult;
    }

    @FunctionalInterface private interface InputStreamSupplier {

        InputStream get() throws IOException;
    }
}
