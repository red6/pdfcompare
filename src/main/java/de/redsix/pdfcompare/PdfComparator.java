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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

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
    private ExecutorService drawExecutor = Executors.newSingleThreadExecutor();
    private ExecutorService parrallelDrawExecutor = Executors.newFixedThreadPool(2);
    private BlockingQueue<DiffImage> diffQueue = new LinkedBlockingQueue<>(4);
    private ExecutorService diffExecutor = Executors.newSingleThreadExecutor();
    private final T compareResult;
    private boolean doneDrawing = false;

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

    public T compare() throws IOException {
        try {
            if (expectedStreamSupplier == null && actualStreamSupplier == null) {
                return compareResult;
            }
            try (final InputStream expectedStream = expectedStreamSupplier.get()) {
                try (final InputStream actualStream = actualStreamSupplier.get()) {
                    try (PDDocument expectedDocument = PDDocument.load(expectedStream)) {
                        PDFRenderer expectedPdfRenderer = new PDFRenderer(expectedDocument);
                        try (PDDocument actualDocument = PDDocument.load(actualStream)) {
                            PDFRenderer actualPdfRenderer = new PDFRenderer(actualDocument);
                            final int minPageCount = Math.min(expectedDocument.getNumberOfPages(), actualDocument.getNumberOfPages());
                            startComparatorThread();
                            CountDownLatch latch = new CountDownLatch(minPageCount);
                            for (int pageIndex = 0; pageIndex < minPageCount; pageIndex++) {
                                drawImage(latch, pageIndex, expectedPdfRenderer, actualPdfRenderer);
                            }
                            Utilities.await(latch, "FullCompare");
                            doneDrawing = true;
                            Utilities.shutdownAndAwaitTermination(drawExecutor, "Draw");
                            Utilities.shutdownAndAwaitTermination(parrallelDrawExecutor, "Parallel Draw");
                            Utilities.shutdownAndAwaitTermination(diffExecutor, "Diff");
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
                    LOG.warn("No files found to compare. Tried Expected: '{}' and Actual: '{}'", ex.getFile(), innerEx.getFile());
                    compareResult.noPagesFound();
                }
            }
        } finally {
            compareResult.done();
        }
        return compareResult;
    }

    private void startComparatorThread() {
        diffExecutor.execute(() -> {
            while (!diffQueue.isEmpty() || !doneDrawing) {
                try {
                    final DiffImage diffImage = diffQueue.take();
                    LOG.debug("Diffing page {}", diffImage);
                    diffImage.diffImages();
                    LOG.debug("DONE Diffing page {}", diffImage);
                } catch (Exception e) {
                    LOG.error("Exception while diffing Images", e);
                }
            }
        });
    }

    private void drawImage(final CountDownLatch latch, final int pageIndex, final PDFRenderer expectedPdfRenderer,
            final PDFRenderer actualPdfRenderer) {
        drawExecutor.execute(() -> {
            try {
                LOG.debug("Drawing page {}", pageIndex);
                final Future<BufferedImage> expectedImageFuture = parrallelDrawExecutor.submit(() -> {
                    return renderPageAsImage(expectedPdfRenderer, pageIndex);
                });
                final Future<BufferedImage> actualImageFuture = parrallelDrawExecutor.submit(() -> {
                    return renderPageAsImage(actualPdfRenderer, pageIndex);
                });
                final DiffImage diffImage = new DiffImage(expectedImageFuture.get(), actualImageFuture.get(), pageIndex, exclusions, compareResult);
                boolean successful = false;
                do {
                    try {
                        LOG.debug("Enqueueing page {}. {} DiffImages in Queue", pageIndex, diffQueue.size());
                        diffQueue.put(diffImage);
                        successful = true;
                    } catch (InterruptedException e) {
                        LOG.warn("Put was interrupted");
                    }
                }
                while (!successful);
                LOG.debug("DONE drawing page {}", pageIndex);
            } catch (InterruptedException e) {
                LOG.error("Waiting for Future was interrupted", e);
            } catch (ExecutionException e) {
                LOG.error("Error while rendering page {}", pageIndex, e);
            } finally {
                latch.countDown();
            }
        });
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

    public T getResult() {
        return compareResult;
    }

    @FunctionalInterface private interface InputStreamSupplier {

        InputStream get() throws IOException;
    }
}
