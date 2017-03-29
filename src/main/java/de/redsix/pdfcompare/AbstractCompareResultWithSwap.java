package de.redsix.pdfcompare;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

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
            LOG.debug("Swapping {} pages to disk", diffImages.size());
            Instant start = Instant.now();

//            try {
//                Path tmpDir = getTempDir();
//                final Iterator<Entry<Integer, BufferedImage>> iterator = diffImages.entrySet().iterator();
//                while (iterator.hasNext()) {
//                    final Entry<Integer, BufferedImage> entry = iterator.next();
//                    if (!keepImages()) {
//                        iterator.remove();
//                    }
//                    ImageIO.write(entry.getValue(), "PNG", tmpDir.resolve(String.format("image_%06d", entry.getKey())).toFile());
//                }
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }

            final Integer minPageIndex = Collections.min(diffImages.keySet());
            try (PDDocument document = new PDDocument()) {
                addImagesToDocument(document);
                final Path tempDir = getTempDir();
                final Path tempFile = tempDir.resolve(String.format("partial_%06d.pdf", minPageIndex));
                document.save(tempFile.toFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Instant end = Instant.now();
            System.out.println("Swap took: " + Duration.between(start, end).toMillis() + "ms");
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
