package de.redsix.pdfcompare;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;

public class DiskUsingCompareResult extends CompareResult {

    private Path tempDir;
    private boolean hasImages = false;

    @Override
    protected void addImagesToDocument(final PDDocument document) throws IOException {
        List<Path> paths = new ArrayList<>();
        try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(tempDir, "image_*")) {
            for (Path path : directoryStream) {
                paths.add(path);
            }
        }
        Collections.sort(paths);
        for (Path path : paths) {
            addPageToDocument(document, ImageIO.read(path.toFile()));
        }
        removeTempDir();
    }

    public void removeTempDir() throws IOException {
        if (Files.exists(tempDir) && Files.isDirectory(tempDir)) {
            Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
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

    public synchronized Path getTempDir() throws IOException {
        if (tempDir == null) {
            tempDir = Files.createTempDirectory("PdfCompare");
            tempDir.toFile().deleteOnExit();
        }
        return tempDir;
    }
}