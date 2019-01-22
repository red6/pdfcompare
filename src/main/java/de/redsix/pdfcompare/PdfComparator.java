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

import static de.redsix.pdfcompare.Utilities.blockingExecutor;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import de.redsix.pdfcompare.env.DefaultEnvironment;
import de.redsix.pdfcompare.env.Environment;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfComparator<T extends CompareResultImpl> {

    private static final Logger LOG = LoggerFactory.getLogger(PdfComparator.class);
    public static final int DPI = 300;
    private static final int EXTRA_RGB = new Color(0, 160, 0).getRGB();
    private static final int MISSING_RGB = new Color(220, 0, 0).getRGB();
    public static final int MARKER_WIDTH = 20;
    private Environment environment;
    private final Exclusions exclusions = new Exclusions();
    private InputStreamSupplier expectedStreamSupplier;
    private InputStreamSupplier actualStreamSupplier;
    private ExecutorService drawExecutor;
    private ExecutorService parrallelDrawExecutor;
    private ExecutorService diffExecutor;
    private final T compareResult;
    private final int timeout = 3;
    private final TimeUnit unit = TimeUnit.MINUTES;
    private String expectedPassword = "";
    private String actualPassword = "";

    private PdfComparator(T compareResult) {
        Objects.requireNonNull(compareResult, "compareResult is null");
        this.compareResult = compareResult;
    }

    public PdfComparator(String expectedPdfFilename, String actualPdfFilename) throws IOException {
        this(expectedPdfFilename, actualPdfFilename, (T) new CompareResultImpl());
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
        this(expectedPath, actualPath, (T) new CompareResultImpl());
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
        this(expectedFile, actualFile, (T) new CompareResultImpl());
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
        this(expectedPdfIS, actualPdfIS, (T) new CompareResultImpl());
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

    public void setEnvironment(Environment environment) {
        this.environment = environment;
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

    public PdfComparator<T> withIgnore(final InputStream ignoreIS) {
        Objects.requireNonNull(ignoreIS, "ignoreIS is null");
        exclusions.readExclusions(ignoreIS);
        return this;
    }

    public PdfComparator<T> with(final PageArea exclusion) {
        Objects.requireNonNull(exclusion, "exclusion is null");
        exclusions.add(exclusion);
        return this;
    }

    public PdfComparator<T> withExpectedPassword(final String password) {
        Objects.requireNonNull(password, "password is null");
        expectedPassword = password;
        return this;
    }

    public PdfComparator<T> withActualPassword(final String password) {
        Objects.requireNonNull(password, "password is null");
        actualPassword = password;
        return this;
    }

    private void buildEnvironment() {
        if (environment == null) {
            environment = DefaultEnvironment.create();
        }
        compareResult.setEnvironment(environment);

        drawExecutor = blockingExecutor("Draw", 1, 50, environment);
        parrallelDrawExecutor = blockingExecutor("ParallelDraw", 2, 4, environment);
        diffExecutor = blockingExecutor("Diff", 1, 2, environment);
    }

    public CompareResult compare() throws IOException {
        try {
            if (expectedStreamSupplier == null || actualStreamSupplier == null) {
                return compareResult;
            }
            buildEnvironment();
            try (final InputStream expectedStream = expectedStreamSupplier.get()) {
                try (final InputStream actualStream = actualStreamSupplier.get()) {
                    try (PDDocument expectedDocument = PDDocument
                            .load(expectedStream, expectedPassword, Utilities.getMemorySettings(environment.getDocumentCacheSize()))) {
                        try (PDDocument actualDocument = PDDocument
                                .load(actualStream, actualPassword, Utilities.getMemorySettings(environment.getDocumentCacheSize()))) {
                            compare(expectedDocument, actualDocument);
                        }
                    }
                } catch (NoSuchFileException ex) {
                    addSingleDocumentToResult(expectedStream, MISSING_RGB);
                    compareResult.expectedOnly();
                }
            } catch (NoSuchFileException ex) {
                try (final InputStream actualStream = actualStreamSupplier.get()) {
                    addSingleDocumentToResult(actualStream, EXTRA_RGB);
                    compareResult.actualOnly();
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

    private void compare(final PDDocument expectedDocument, final PDDocument actualDocument) throws IOException {
        expectedDocument.setResourceCache(new ResourceCacheWithLimitedImages(environment));
        PDFRenderer expectedPdfRenderer = new PDFRenderer(expectedDocument);

        actualDocument.setResourceCache(new ResourceCacheWithLimitedImages(environment));
        PDFRenderer actualPdfRenderer = new PDFRenderer(actualDocument);

        final int minPageCount = Math.min(expectedDocument.getNumberOfPages(), actualDocument.getNumberOfPages());
        CountDownLatch latch = new CountDownLatch(minPageCount);
        for (int pageIndex = 0; pageIndex < minPageCount; pageIndex++) {
            drawImage(latch, pageIndex, expectedDocument, actualDocument, expectedPdfRenderer, actualPdfRenderer);
        }
        Utilities.await(latch, "FullCompare", environment);
        Utilities.shutdownAndAwaitTermination(drawExecutor, "Draw");
        Utilities.shutdownAndAwaitTermination(parrallelDrawExecutor, "Parallel Draw");
        Utilities.shutdownAndAwaitTermination(diffExecutor, "Diff");
        if (expectedDocument.getNumberOfPages() > minPageCount) {
            addExtraPages(expectedDocument, expectedPdfRenderer, minPageCount, MISSING_RGB, true);
        } else if (actualDocument.getNumberOfPages() > minPageCount) {
            addExtraPages(actualDocument, actualPdfRenderer, minPageCount, EXTRA_RGB, false);
        }
    }

    private void drawImage(final CountDownLatch latch, final int pageIndex,
            final PDDocument expectedDocument, final PDDocument actualDocument,
            final PDFRenderer expectedPdfRenderer, final PDFRenderer actualPdfRenderer) {
        drawExecutor.execute(() -> {
            try {
                LOG.trace("Drawing page {}", pageIndex);
                final Future<ImageWithDimension> expectedImageFuture = parrallelDrawExecutor
                        .submit(() -> renderPageAsImage(expectedDocument, expectedPdfRenderer, pageIndex));
                final Future<ImageWithDimension> actualImageFuture = parrallelDrawExecutor
                        .submit(() -> renderPageAsImage(actualDocument, actualPdfRenderer, pageIndex));
                final ImageWithDimension expectedImage = getImage(expectedImageFuture, pageIndex, "expected document");
                final ImageWithDimension actualImage = getImage(actualImageFuture, pageIndex, "actual document");
                final DiffImage diffImage = new DiffImage(expectedImage, actualImage, pageIndex, environment, exclusions, compareResult);
                LOG.trace("Enqueueing page {}.", pageIndex);
                diffExecutor.execute(() -> {
                    LOG.trace("Diffing page {}", diffImage);
                    diffImage.diffImages();
                    LOG.trace("DONE Diffing page {}", diffImage);
                });
                LOG.trace("DONE drawing page {}", pageIndex);
            } catch (RenderingException e) {
            } finally {
                latch.countDown();
            }
        });
    }

    private ImageWithDimension getImage(final Future<ImageWithDimension> imageFuture, final int pageIndex, final String type) {
        try {
            return imageFuture.get(timeout, unit);
        } catch (InterruptedException e) {
            LOG.warn("Waiting for Future was interrupted while rendering page {} for {}", pageIndex, type, e);
            Thread.currentThread().interrupt();
        } catch (TimeoutException e) {
            LOG.error("Waiting for Future timed out after {} {} while rendering page {} for {}", timeout, unit, pageIndex, type, e);
        } catch (ExecutionException e) {
            LOG.error("Error while rendering page {} for {}", pageIndex, type, e);
        }
        throw new RenderingException();
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
            ImageWithDimension image = renderPageAsImage(document, pdfRenderer, pageIndex);
            final DataBuffer dataBuffer = image.bufferedImage.getRaster().getDataBuffer();
            for (int i = 0; i < image.bufferedImage.getWidth() * MARKER_WIDTH; i++) {
                dataBuffer.setElem(i, color);
            }
            for (int i = 0; i < image.bufferedImage.getHeight(); i++) {
                for (int j = 0; j < MARKER_WIDTH; j++) {
                    dataBuffer.setElem(i * image.bufferedImage.getWidth() + j, color);
                }
            }
            if (expected) {
                compareResult.addPage(new PageDiffCalculator(true, false), pageIndex, image, blank(image), image);
            } else {
                compareResult.addPage(new PageDiffCalculator(true, false), pageIndex, blank(image), image, image);
            }
        }
    }

    private static ImageWithDimension blank(final ImageWithDimension image) {
        return new ImageWithDimension(new BufferedImage(image.bufferedImage.getWidth(), image.bufferedImage.getHeight(), image.bufferedImage.getType()), image.width, image.height);
    }

    public static ImageWithDimension renderPageAsImage(final PDDocument document, final PDFRenderer expectedPdfRenderer, final int pageIndex)
            throws IOException {
        final BufferedImage bufferedImage = expectedPdfRenderer.renderImageWithDPI(pageIndex, DPI);
        final PDPage page = document.getPage(pageIndex);
        final PDRectangle mediaBox = page.getMediaBox();
        if (page.getRotation() == 90 || page.getRotation() == 270)
            return new ImageWithDimension(bufferedImage, mediaBox.getHeight(), mediaBox.getWidth());
        else
            return new ImageWithDimension(bufferedImage, mediaBox.getWidth(), mediaBox.getHeight());
    }

    public T getResult() {
        return compareResult;
    }

    @FunctionalInterface private interface InputStreamSupplier {

        InputStream get() throws IOException;
    }
}
