package de.redsix.pdfcompare;

import static de.redsix.pdfcompare.Utilities.blockingExecutor;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This CompareResult monitors the memory the JVM consumes through Runtime.totalMemory() - Runtime.freeMemory()
 * when a new page is added. When the consumed memory crosses a threshold, images are swapped to disk and removed
 * from memory. The threshold defaults to 70% of Runtime.maxMemory() but at least 200MB, which worked for me.
 * After swapping, a System.gc() is triggered.
 */
public abstract class AbstractCompareResultWithSwap extends CompareResult {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCompareResultWithSwap.class);
    private Path tempDir;
    private boolean hasImages = false;
    private boolean swapped;
    private ExecutorService swapExecutor;

    @Override
    public boolean writeTo(final String filename) {
        if (!swapped) {
            return super.writeTo(filename);
        }
        swapToDisk();
        Utilities.shutdownAndAwaitTermination(swapExecutor, "Swap");
        try {
            LOG.trace("Merging...");
            Instant start = Instant.now();
            final PDFMergerUtility mergerUtility = new PDFMergerUtility();
            mergerUtility.setDestinationFileName(filename + ".pdf");
            for (Path path : FileUtils.getPaths(getTempDir(), "partial_*")) {
                mergerUtility.addSource(path.toFile());
            }
            mergerUtility.mergeDocuments(Utilities.getMemorySettings(Environment.getMergeCacheSize()));
            Instant end = Instant.now();
            LOG.trace("Merging took: " + Duration.between(start, end).toMillis() + "ms");
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (tempDir != null) {
                FileUtils.removeTempDir(tempDir);
            }
        }
        return isEqual;
    }

    @Override
    public synchronized void addPage(final PageDiffCalculator diffCalculator, final int pageIndex,
            final ImageWithDimension expectedImage, final ImageWithDimension actualImage, final ImageWithDimension diffImage) {
        super.addPage(diffCalculator, pageIndex, expectedImage, actualImage, diffImage);
        hasImages = true;
        if (needToSwap()) {
            swapToDisk();
            afterSwap();
        }
    }

    protected void afterSwap() {
    }

    protected abstract boolean needToSwap();

    private synchronized Executor getExecutor() {
        if (swapExecutor == null) {
            swapExecutor = blockingExecutor("Swap", 0, 2, 1);
        }
        return swapExecutor;
    }

    private synchronized void swapToDisk() {
        if (!diffImages.isEmpty()) {
            final Map<Integer, ImageWithDimension> images = new TreeMap<>();
            final Iterator<Entry<Integer, ImageWithDimension>> iterator = diffImages.entrySet().iterator();
            int previousPage = diffImages.keySet().iterator().next();
            while (iterator.hasNext()) {
                final Entry<Integer, ImageWithDimension> entry = iterator.next();
                if (entry.getKey() <= previousPage + 1) {
                    images.put(entry.getKey(), entry.getValue());
                    iterator.remove();
                    previousPage = entry.getKey();
                }
            }
            if (!images.isEmpty()) {
                swapped = true;
                getExecutor().execute(() -> {
                    LOG.trace("Swapping {} pages to disk", images.size());
                    Instant start = Instant.now();

                    final int minPageIndex = images.keySet().iterator().next();
                    LOG.trace("minPageIndex: {}", minPageIndex);
                    try (PDDocument document = new PDDocument(Utilities.getMemorySettings(Environment.getSwapCacheSize()))) {
                        document.setResourceCache(new ResourceCacheWithLimitedImages());
                        addImagesToDocument(document, images);
                        final Path tempDir = getTempDir();
                        final Path tempFile = tempDir.resolve(String.format("partial_%06d.pdf", minPageIndex));
                        document.save(tempFile.toFile());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Instant end = Instant.now();
                    LOG.trace("Swapping took: {}ms", Duration.between(start, end).toMillis());
                });
            }
        }
    }

    @Override
    protected boolean hasImages() {
        return hasImages;
    }

    private synchronized Path getTempDir() throws IOException {
        if (tempDir == null) {
            tempDir = FileUtils.createTempDir("PdfCompare");
        }
        return tempDir;
    }

    @Override
    protected void finalize() throws Throwable {
        if (swapExecutor != null) {
            swapExecutor.shutdown();
        }
    }
}
