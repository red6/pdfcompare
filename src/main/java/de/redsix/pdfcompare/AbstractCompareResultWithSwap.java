package de.redsix.pdfcompare;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.pdfbox.io.MemoryUsageSetting;
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
    private ExecutorService swapExecutor = Executors.newFixedThreadPool(5);

    @Override
    public boolean writeTo(final String filename) {
        if (!swapped) {
            return super.writeTo(filename);
        }
        swapToDisk();
        Utilities.shutdownAndAwaitTermination(swapExecutor, "Swap");
        try {
            LOG.debug("Merging...");
            Instant start = Instant.now();
            final PDFMergerUtility mergerUtility = new PDFMergerUtility();
            mergerUtility.setDestinationFileName(filename + ".pdf");
            for (Path path : FileUtils.getPaths(getTempDir(), "partial_*")) {
                mergerUtility.addSource(path.toFile());
            }
            mergerUtility.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
            Instant end = Instant.now();
            System.out.println("Merging took: " + Duration.between(start, end).toMillis() + "ms");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return isEqual;
    }

    @Override
    public synchronized void addPage(final boolean hasDifferences, final boolean hasDifferenceInExclusion, final int pageIndex,
            final BufferedImage expectedImage, final BufferedImage actualImage, final BufferedImage diffImage) {
        super.addPage(hasDifferences, hasDifferenceInExclusion, pageIndex, expectedImage, actualImage, diffImage);
        hasImages = true;
        if (needToSwap()) {
            swapToDisk();
            afterSwap();
        }
    }

    protected void afterSwap() {
    }

    protected abstract boolean needToSwap();

    private synchronized void swapToDisk() {
        if (!diffImages.isEmpty()) {
            final Map<Integer, BufferedImage> images = new TreeMap<>();
            final Iterator<Entry<Integer, BufferedImage>> iterator = diffImages.entrySet().iterator();
            int previousPage = diffImages.keySet().iterator().next();
            while (iterator.hasNext()) {
                final Entry<Integer, BufferedImage> entry = iterator.next();
                if (entry.getKey() <= previousPage + 1) {
                    images.put(entry.getKey(), entry.getValue());
                    iterator.remove();
                    previousPage = entry.getKey();
                }
            }
            if (!images.isEmpty()) {
                swapped = true;
                swapExecutor.execute(() -> {
                    LOG.debug("Swapping {} pages to disk", images.size());
                    Instant start = Instant.now();

                    final int minPageIndex = images.keySet().iterator().next();
                    LOG.debug("minPageIndex: {}", minPageIndex);
                    try (PDDocument document = new PDDocument()) {
                        addImagesToDocument(document, images);
                        final Path tempDir = getTempDir();
                        final Path tempFile = tempDir.resolve(String.format("partial_%06d.pdf", minPageIndex));
                        document.save(tempFile.toFile());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Instant end = Instant.now();
                    System.out.println("Swap took: " + Duration.between(start, end).toMillis() + "ms");
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
}
