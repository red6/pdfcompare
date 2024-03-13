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

import de.redsix.pdfcompare.env.ConfigFileEnvironment;
import de.redsix.pdfcompare.env.DefaultEnvironment;
import de.redsix.pdfcompare.env.Environment;
import de.redsix.pdfcompare.env.SimpleEnvironment;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * The PdfComparator is the entry point to use for comparing documents.
 * It allows to specify which documents to compare and which additional features to apply, like
 * ignores and passwords.
 * One PdfCompare object is created for every comparison and they are not reused.
 *
 * @param <T> Allows to specify different CompareResults.
 */
public class PdfComparator<T extends CompareResultImpl> {

    private static final Logger LOG = LoggerFactory.getLogger(PdfComparator.class);

    private static final int TIMEOUT = 3;
    private static final TimeUnit unit = TimeUnit.MINUTES;
    public static final int MARKER_WIDTH = 20;

    private Environment environment;
    private Exclusions exclusions;
    private InputStreamSupplier expectedStreamSupplier;
    private InputStreamSupplier actualStreamSupplier;
    private ExecutorService drawExecutor;
    private ExecutorService parrallelDrawExecutor;
    private ExecutorService diffExecutor;
    private final T compareResult;
    private String expectedPassword = "";
    private String actualPassword = "";
    private boolean withIgnoreCalled = false;
    private final ConcurrentLinkedQueue<Throwable> exceptionFromOtherThread = new ConcurrentLinkedQueue<>();

    /**
     * Compare two PDFs, that are given as base64 encoded strings.
     *
     * @param expectedPdfBase64 expected PDF in base64 encoded format
     * @param actualPdfBase64   actual PDF in base64 encoded format
     * @return A CompareResultImpl object, that contains the result of this compare.
     */
    public static <T extends CompareResultImpl> PdfComparator base64(String expectedPdfBase64, String actualPdfBase64) {
        return base64(expectedPdfBase64, actualPdfBase64, new CompareResultImpl());
    }

    /**
     * Compare two PDFs, that are given as base64 encoded strings.
     *
     * @param expectedPdfBase64 expected PDF in base64 encoded format
     * @param actualPdfBase64   actual PDF in base64 encoded format
     * @param compareResult     the CompareResult to use during this compare. Allows to provide CompareResultImpl Subtypes with Swapping for example.
     * @return A CompareResultImpl object, that contains the result of this compare.
     */
    public static <T extends CompareResultImpl> PdfComparator base64(String expectedPdfBase64, String actualPdfBase64, T compareResult) {
        PdfComparator pdfComparator = new PdfComparator<>(compareResult);
        pdfComparator.expectedStreamSupplier = () -> new ByteArrayInputStream(Base64.getDecoder().decode(expectedPdfBase64));
        pdfComparator.actualStreamSupplier = () -> new ByteArrayInputStream(Base64.getDecoder().decode(actualPdfBase64));
        return pdfComparator;
    }

    private PdfComparator(T compareResult) {
        Objects.requireNonNull(compareResult, "compareResult is null");
        this.compareResult = compareResult;
    }

    /**
     * Compare two PDFs by providing two filenames for the expected PDF and the actual PDF.
     *
     * @param expectedPdfFilename filename for the expected PDF
     * @param actualPdfFilename   filename for the actual PDF
     */
    public PdfComparator(String expectedPdfFilename, String actualPdfFilename) {
        this(expectedPdfFilename, actualPdfFilename, (T) new CompareResultImpl());
    }

    /**
     * Compare two PDFs by providing two filenames for the expected PDF and the actual PDF.
     *
     * @param expectedPdfFilename filename for the expected PDF
     * @param actualPdfFilename   filename for the actual PDF
     * @param compareResult       the CompareResult to use during this compare. Allows to provide CompareResultImpl Subtypes with Swapping for example.
     */
    public PdfComparator(String expectedPdfFilename, String actualPdfFilename, T compareResult) {
        this(compareResult);
        Objects.requireNonNull(expectedPdfFilename, "expectedPdfFilename is null");
        Objects.requireNonNull(actualPdfFilename, "actualPdfFilename is null");
        if (!expectedPdfFilename.equals(actualPdfFilename)) {
            this.expectedStreamSupplier = () -> Files.newInputStream(Paths.get(expectedPdfFilename));
            this.actualStreamSupplier = () -> Files.newInputStream(Paths.get(actualPdfFilename));
        }
    }

    /**
     * Compare two PDFs by providing two Path objects for the expected PDF and the actual PDF.
     *
     * @param expectedPath Path for the expected PDF
     * @param actualPath   Path for the actual PDF
     */
    public PdfComparator(final Path expectedPath, final Path actualPath) {
        this(expectedPath, actualPath, (T) new CompareResultImpl());
    }

    /**
     * Compare two PDFs by providing two Path objects for the expected PDF and the actual PDF.
     *
     * @param expectedPath  Path for the expected PDF
     * @param actualPath    Path for the actual PDF
     * @param compareResult the CompareResult to use during this compare. Allows to provide CompareResultImpl Subtypes with Swapping for example.
     */
    public PdfComparator(final Path expectedPath, final Path actualPath, final T compareResult) {
        this(compareResult);
        Objects.requireNonNull(expectedPath, "expectedPath is null");
        Objects.requireNonNull(actualPath, "actualPath is null");
        if (!expectedPath.equals(actualPath)) {
            this.expectedStreamSupplier = () -> Files.newInputStream(expectedPath);
            this.actualStreamSupplier = () -> Files.newInputStream(actualPath);
        }
    }

    /**
     * Compare two PDFs by providing two File objects for the expected PDF and the actual PDF.
     *
     * @param expectedFile File for the expected PDF
     * @param actualFile   File for the actual PDF
     */
    public PdfComparator(final File expectedFile, final File actualFile) {
        this(expectedFile, actualFile, (T) new CompareResultImpl());
    }

    /**
     * Compare two PDFs by providing two File objects for the expected PDF and the actual PDF.
     *
     * @param expectedFile  File for the expected PDF
     * @param actualFile    File for the actual PDF
     * @param compareResult the CompareResult to use during this compare. Allows to provide CompareResultImpl Subtypes with Swapping for example.
     */
    public PdfComparator(final File expectedFile, final File actualFile, final T compareResult) {
        this(compareResult);
        Objects.requireNonNull(expectedFile, "expectedFile is null");
        Objects.requireNonNull(actualFile, "actualFile is null");
        if (!expectedFile.equals(actualFile)) {
            this.expectedStreamSupplier = () -> new FileInputStream(expectedFile);
            this.actualStreamSupplier = () -> new FileInputStream(actualFile);
        }
    }

    /**
     * Compare two PDFs by providing two InputStream objects for the expected PDF and the actual PDF.
     *
     * @param expectedPdfIS InputStream for the expected PDF
     * @param actualPdfIS   InputStream for the actual PDF
     */
    public PdfComparator(final InputStream expectedPdfIS, final InputStream actualPdfIS) {
        this(expectedPdfIS, actualPdfIS, (T) new CompareResultImpl());
    }

    /**
     * Compare two PDFs by providing two InputStream objects for the expected PDF and the actual PDF.
     *
     * @param expectedPdfIS InputStream for the expected PDF
     * @param actualPdfIS   InputStream for the actual PDF
     * @param compareResult the CompareResult to use during this compare. Allows to provide CompareResultImpl Subtypes with Swapping for example.
     */
    public PdfComparator(final InputStream expectedPdfIS, final InputStream actualPdfIS, final T compareResult) {
        this(compareResult);
        Objects.requireNonNull(expectedPdfIS, "expectedPdfIS is null");
        Objects.requireNonNull(actualPdfIS, "actualPdfIS is null");
        if (!expectedPdfIS.equals(actualPdfIS)) {
            this.expectedStreamSupplier = () -> expectedPdfIS;
            this.actualStreamSupplier = () -> actualPdfIS;
        }
    }

    /**
     * @deprecated use {@link #withEnvironment(Environment)} instead.
     */
    @Deprecated
    public void setEnvironment(Environment environment) {
        withEnvironment(environment);
    }

    /**
     * Allows to inject an Environment that can override environment settings.
     * {@link SimpleEnvironment} is particularly useful if you want to override some properties.
     * If you want to specify your own config file, instead of the default application.conf
     * in the root of the classpath, you an use a {@link ConfigFileEnvironment}.
     *
     * @param environment the environment so use
     * @return this
     * @throws IllegalStateException when withIgnore methods are called before this method.
     */
    public PdfComparator<T> withEnvironment(Environment environment) {
        if (withIgnoreCalled) {
            throw new IllegalStateException("withEnvironment(...) must be called before any withIgnore(...) methods are called.");
        }
        this.environment = environment;
        return this;
    }

    /**
     * Reads a file with Exclusions.
     *
     * @param ignoreFilename The file to read
     * @return this
     * @see PdfComparator#withIgnore(Path)
     */
    public PdfComparator<T> withIgnore(final String ignoreFilename) {
        Objects.requireNonNull(ignoreFilename, "ignoreFilename is null");
        withIgnoreCalled = true;
        getExclusions().readExclusions(ignoreFilename);
        return this;
    }

    /**
     * Reads a file with Exclusions.
     *
     * @param ignoreFile The file to read
     * @return this
     * @see PdfComparator#withIgnore(Path)
     */
    public PdfComparator<T> withIgnore(final File ignoreFile) {
        Objects.requireNonNull(ignoreFile, "ignoreFile is null");
        withIgnoreCalled = true;
        getExclusions().readExclusions(ignoreFile);
        return this;
    }

    /**
     * Reads a file with Exclusions.
     * <p>
     * It is possible to define rectangular areas that are ignored during comparison. For that, a file needs to be created, which defines areas to ignore.
     * The file format is JSON (or actually a superset called <a href="https://github.com/lightbend/config/blob/master/HOCON.md">HOCON</a>) and has the following form:
     * <pre>
     * exclusions: [
     *     {
     *         page: 2
     *         x1: 300 // entries without a unit are in pixels, when Pdf is rendered at 300DPI
     *         y1: 1000
     *         x2: 550
     *         y2: 1300
     *     },
     *     {
     *         // page is optional. When not given, the exclusion applies to all pages.
     *         x1: 130.5mm // entries can also be given in units of cm, mm or pt (DTP-Point defined as 1/72 Inches)
     *         y1: 3.3cm
     *         x2: 190mm
     *         y2: 3.7cm
     *     },
     *     {
     *         page: 7
     *         // coordinates are optional. When not given, the whole page is excluded.
     *     }
     * ]</pre>
     *
     * @param ignorePath The file to read
     * @return this
     */
    public PdfComparator<T> withIgnore(final Path ignorePath) {
        Objects.requireNonNull(ignorePath, "ignorePath is null");
        withIgnoreCalled = true;
        getExclusions().readExclusions(ignorePath);
        return this;
    }

    /**
     * Reads Exclusions from an InputStream.
     *
     * @param ignoreIS The file to read
     * @return this
     * @see PdfComparator#withIgnore(Path)
     */
    public PdfComparator<T> withIgnore(final InputStream ignoreIS) {
        Objects.requireNonNull(ignoreIS, "ignoreIS is null");
        withIgnoreCalled = true;
        getExclusions().readExclusions(ignoreIS);
        return this;
    }

    /**
     * Allows to specify an area of a page that is excluded during the comparison.
     *
     * @param exclusion An area of the document, that shall be ignored.
     * @return this
     */
    public PdfComparator<T> withIgnore(final PageArea exclusion) {
        Objects.requireNonNull(exclusion, "exclusion is null");
        withIgnoreCalled = true;
        getExclusions().add(exclusion);
        return this;
    }

    /**
     * Allows to specify an area of a page that is excluded during the comparison.
     *
     * @param exclusion An area of the document, that shall be ignored.
     * @return this
     * @deprecated Use {@link PdfComparator#withIgnore(PageArea)} instead.
     */
    @Deprecated
    public PdfComparator<T> with(final PageArea exclusion) {
        return withIgnore(exclusion);
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

    private Exclusions getExclusions() {
        if (exclusions == null) {
            exclusions = new Exclusions(getEnvironment());
        }
        return exclusions;
    }

    private Environment getEnvironment() {
        if (environment == null) {
            environment = DefaultEnvironment.create();
        }
        return environment;
    }

    private void buildEnvironment() {
        compareResult.setEnvironment(getEnvironment());

        drawExecutor = blockingExecutor("Draw", 1, 50, environment);
        parrallelDrawExecutor = blockingExecutor("ParallelDraw", 2, 4, environment);
        diffExecutor = blockingExecutor("Diff", 1, 2, environment);
    }

    /**
     * Does the actual comparison of the given PDF documents.
     * When errors occur during the rendering or diffing of pages, they are collected and added to
     * a RenderingException as SuppressedExceptions.
     *
     * @return the CompareResult gives information about the comparison
     * @throws IOException        when an input file or stream can not be read
     * @throws RenderingException when errors during rendering or diffing of pages occurred
     */
    public T compare() throws IOException, RenderingException {
        try {
            if (expectedStreamSupplier == null || actualStreamSupplier == null) {
                return compareResult;
            }
            buildEnvironment();
            try (final InputStream expectedInputStream = expectedStreamSupplier.get()) {
                try (final RandomAccessRead expectedStream = new RandomAccessReadBuffer(expectedInputStream)) {
                    try (final InputStream actualInputStream = actualStreamSupplier.get()) {
                        try (final RandomAccessRead actualStream = new RandomAccessReadBuffer(actualInputStream)) {
                            try (PDDocument expectedDocument = Loader
                                    .loadPDF(expectedStream, expectedPassword, Utilities.getMemorySettings(environment.getDocumentCacheSize()))) {
                                try (PDDocument actualDocument = Loader
                                        .loadPDF(actualStream, actualPassword, Utilities.getMemorySettings(environment.getDocumentCacheSize()))) {
                                    compare(expectedDocument, actualDocument);
                                }
                            }
                        } catch (NoSuchFileException ex) {
                            addSingleDocumentToResult(expectedStream, environment.getActualColor().getRGB());
                            compareResult.expectedOnly();
                        }
                    }
                } catch (NoSuchFileException ex) {
                    try (final RandomAccessRead actualStream = new RandomAccessReadBuffer(actualStreamSupplier.get())) {
                        addSingleDocumentToResult(actualStream, environment.getExpectedColor().getRGB());
                        compareResult.actualOnly();
                    } catch (NoSuchFileException innerEx) {
                        LOG.warn("No files found to compare. Tried Expected: '{}' and Actual: '{}'", ex.getFile(), innerEx.getFile());
                        compareResult.noPagesFound();
                    }
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
        Utilities.shutdownAndAwaitTermination(drawExecutor, "Draw", environment);
        Utilities.shutdownAndAwaitTermination(parrallelDrawExecutor, "Parallel Draw", environment);
        Utilities.shutdownAndAwaitTermination(diffExecutor, "Diff", environment);
        if (expectedDocument.getNumberOfPages() > minPageCount) {
            addExtraPages(expectedDocument, expectedPdfRenderer, minPageCount, environment.getActualColor().getRGB(), true);
        } else if (actualDocument.getNumberOfPages() > minPageCount) {
            addExtraPages(actualDocument, actualPdfRenderer, minPageCount, environment.getExpectedColor().getRGB(), false);
        }
        if (!exceptionFromOtherThread.isEmpty()) {
            RenderingException ex = new RenderingException("Exceptions were caught during rendering or diffing");
            exceptionFromOtherThread.forEach(ex::addSuppressed);
            throw ex;
        }
    }

    private void drawImage(final CountDownLatch latch, final int pageIndex,
            final PDDocument expectedDocument, final PDDocument actualDocument,
            final PDFRenderer expectedPdfRenderer, final PDFRenderer actualPdfRenderer) {
        drawExecutor.execute(() -> {
            try {
                LOG.trace("Drawing page {}", pageIndex);
                final Future<ImageWithDimension> expectedImageFuture = parrallelDrawExecutor
                        .submit(() -> renderPageAsImage(expectedDocument, expectedPdfRenderer, pageIndex, environment));
                final Future<ImageWithDimension> actualImageFuture = parrallelDrawExecutor
                        .submit(() -> renderPageAsImage(actualDocument, actualPdfRenderer, pageIndex, environment));
                final ImageWithDimension expectedImage = getImage(expectedImageFuture, pageIndex, "expected document");
                final ImageWithDimension actualImage = getImage(actualImageFuture, pageIndex, "actual document");
                final DiffImage diffImage = new DiffImage(expectedImage, actualImage, pageIndex, environment, getExclusions(), compareResult);
                LOG.trace("Enqueueing page {}.", pageIndex);
                diffExecutor.execute(() -> {
                    LOG.trace("Diffing page {}", diffImage);
                    try {
                        diffImage.diffImages();
                    } catch (Throwable t) {
                        addErrorPage(pageIndex, "An error occurred, while diffing this page", t);
                    }
                    LOG.trace("DONE Diffing page {}", diffImage);
                });
                LOG.trace("DONE drawing page {}", pageIndex);
            } catch (Throwable t) {
                addErrorPage(pageIndex, "An error occurred, while rendering this page", t);
            } finally {
                latch.countDown();
            }
        });
    }

    private void addErrorPage(int pageIndex, String message, Throwable t) {
        LOG.error(message, t);
        exceptionFromOtherThread.add(t);
        StacktraceImage stacktraceImage = new StacktraceImage(message, t, environment);
        ImageWithDimension errorImage = stacktraceImage.getImage();
        compareResult.addPage(new PageDiffCalculator(new PageArea(pageIndex + 1)), pageIndex, stacktraceImage.getBlankImage(), errorImage, errorImage);
    }

    private ImageWithDimension getImage(final Future<ImageWithDimension> imageFuture, final int pageIndex, final String type) {
        try {
            return imageFuture.get(TIMEOUT, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RenderingException("Waiting for Future was interrupted while rendering page " + (pageIndex + 1) + " of " + type, e);
        } catch (TimeoutException e) {
            String msg = String.format("Waiting for Future timed out after %d %s while rendering page %d of %s", TIMEOUT, unit, pageIndex + 1, type);
            throw new RenderingException(msg, e);
        } catch (ExecutionException e) {
            throw new RenderingException("Error while rendering page " + (pageIndex + 1) + " of " + type, e);
        }
    }

    private void addSingleDocumentToResult(RandomAccessRead expectedPdfIS, int markerColor) throws IOException {
        try (PDDocument expectedDocument = Loader.loadPDF(expectedPdfIS)) {
            PDFRenderer expectedPdfRenderer = new PDFRenderer(expectedDocument);
            addExtraPages(expectedDocument, expectedPdfRenderer, 0, markerColor, true);
        }
    }

    private void addExtraPages(final PDDocument document, final PDFRenderer pdfRenderer, final int minPageCount,
            final int color, final boolean expected) throws IOException {
        for (int pageIndex = minPageCount; pageIndex < document.getNumberOfPages(); pageIndex++) {
            ImageWithDimension image = renderPageAsImage(document, pdfRenderer, pageIndex, environment);
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
                compareResult.addPage(new PageDiffCalculator(new PageArea(pageIndex + 1)), pageIndex, image, blank(image), image);
            } else {
                compareResult.addPage(new PageDiffCalculator(new PageArea(pageIndex + 1)), pageIndex, blank(image), image, image);
            }
        }
    }

    private static ImageWithDimension blank(final ImageWithDimension image) {
        return new ImageWithDimension(
                new BufferedImage(image.bufferedImage.getWidth(), image.bufferedImage.getHeight(), image.bufferedImage.getType()),
                image.width, image.height);
    }

    public static ImageWithDimension renderPageAsImage(final PDDocument document, final PDFRenderer expectedPdfRenderer, final int pageIndex,
            Environment environment) throws IOException {
        final BufferedImage bufferedImage = expectedPdfRenderer.renderImageWithDPI(pageIndex, environment.getDPI());
        final PDPage page = document.getPage(pageIndex);
        final PDRectangle mediaBox = page.getMediaBox();
        if (page.getRotation() == 90 || page.getRotation() == 270) {
            return new ImageWithDimension(bufferedImage, mediaBox.getHeight(), mediaBox.getWidth());
        } else {
            return new ImageWithDimension(bufferedImage, mediaBox.getWidth(), mediaBox.getHeight());
        }
    }

    public T getResult() {
        return compareResult;
    }

    @FunctionalInterface
    private interface InputStreamSupplier {

        InputStream get() throws IOException;
    }
}
