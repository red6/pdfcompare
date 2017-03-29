package de.redsix.pdfcompare;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;

public class CompareResultWithDiskStorage extends CompareResult {

    private Path tempDir;
    private boolean hasImages = false;

    @Override
    protected void addImagesToDocument(final PDDocument document) throws IOException {
        for (Path path : FileUtils.getPaths(getTempDir(), "image_*")) {
            addPageToDocument(document, ImageIO.read(path.toFile()));
        }
        FileUtils.removeTempDir(getTempDir());
    }

    @Override
    public synchronized void addPage(final boolean hasDifferences, final boolean hasDifferenceInExclusion, final int pageIndex,
            final BufferedImage expectedImage, final BufferedImage actualImage, final BufferedImage diffImage) {
        Objects.requireNonNull(expectedImage, "expectedImage is null");
        Objects.requireNonNull(actualImage, "actualImage is null");
        Objects.requireNonNull(diffImage, "diffImage is null");
        hasImages = true;
        this.hasDifferenceInExclusion |= hasDifferenceInExclusion;
        if (hasDifferences) {
            isEqual = false;
        }
        storeImage(pageIndex, diffImage);
    }

    @Override
    protected boolean hasImages() {
        return hasImages;
    }

    private void storeImage(final int pageIndex, final BufferedImage diffImage) {
        try {
            Path tmpDir = getTempDir();
            ImageIO.write(diffImage, "PNG", tmpDir.resolve(String.format("image_%06d", pageIndex)).toFile());
        } catch (IOException e) {
            throw new RuntimeException("Could not write image to Temp Dir", e);
        }
    }

    private synchronized Path getTempDir() throws IOException {
        if (tempDir == null) {
            tempDir = FileUtils.createTempDir("PdfCompare");
        }
        return tempDir;
    }
}