package de.redsix.pdfcompare;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.NoSuchElementException;

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
public class CompareResultWithDiskOverflow extends CompareResult {

    private static final Logger LOG = LoggerFactory.getLogger(CompareResultWithDiskOverflow.class);
    private Path tempDir;
    private boolean hasImages = false;
    private long maxMemoryUsage = Math.max(Math.round(Runtime.getRuntime().maxMemory() * 0.7), 200 * 1024 * 1024);
    private boolean swapped;

    public CompareResultWithDiskOverflow() {
    }

    public CompareResultWithDiskOverflow(final int approximateMaxMemoryUsageInMegaBytes) {
        this.maxMemoryUsage = approximateMaxMemoryUsageInMegaBytes * 1024 * 1024;
    }

    @Override
    public synchronized boolean writeTo(final String filename) {
        if (!swapped) {
            return super.writeTo(filename);
        }
        swapToDisk();
        try {
            final PDFMergerUtility mergerUtility = new PDFMergerUtility();
            mergerUtility.setDestinationFileName(filename + ".pdf");
            for (Path path : FileUtils.getPaths(getTempDir(), "partial_*")) {
                mergerUtility.addSource(path.toFile());
            }
            mergerUtility.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
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
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        if (usedMemory >= maxMemoryUsage) {
            swapToDisk();
            System.gc();
        }
    }

    private synchronized void swapToDisk() {
        if (!diffImages.isEmpty()) {
            LOG.debug("Swapping to disk");
            LOG.debug("DiffImages[{}]: {}", diffImages.size(), diffImages);
            final Integer minPageIndex = Collections.min(diffImages.keySet());
            try (PDDocument document = new PDDocument()) {
                addImagesToDocument(document);
                final Path tempDir = getTempDir();
                final Path tempFile = tempDir.resolve(String.format("partial_%06d.pdf", minPageIndex));
                document.save(tempFile.toFile());
            } catch (NoSuchElementException e) {
                LOG.error("DiffImages: {}", diffImages);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            swapped = true;
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
