package de.redsix.pdfcompare;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);
    private static Collection<Path> tempDirs = new ConcurrentLinkedQueue<>();
    private static volatile boolean shutdownRegistered;
    private static Path tempDirParent;

    public static void setTempDirParent(final Path tempDirParentPath) {
        tempDirParent = tempDirParentPath;
    }

    private static synchronized void addShutdownHook() {
        if (!shutdownRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> removeTempDirs()));
            shutdownRegistered = true;
        }
    }

    private static void removeTempDirs() {
        tempDirs.forEach(FileUtils::removeTempDir);
    }

    public static Path createTempDir(final String prefix) throws IOException {
        final Path tempDir = tempDirParent != null ? Files.createTempDirectory(tempDirParent, prefix) : Files.createTempDirectory(prefix);
        tempDirs.add(tempDir);
        addShutdownHook();
        return tempDir;
    }

    public static void removeTempDir(final Path tempDir) {
        tempDirs.remove(tempDir);
        if (Files.exists(tempDir) && Files.isDirectory(tempDir)) {
            try {
                Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.deleteIfExists(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.deleteIfExists(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                LOG.warn("Error removing temporary directory: {}", tempDir, e);
            }
        }
    }

    public static List<Path> getPaths(final Path dir, final String glob) throws IOException {
        List<Path> paths = new ArrayList<>();
        try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(dir, glob)) {
            for (Path path : directoryStream) {
                paths.add(path);
            }
        }
        Collections.sort(paths);
        return paths;
    }
}
