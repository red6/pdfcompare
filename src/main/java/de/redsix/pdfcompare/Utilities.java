package de.redsix.pdfcompare;

import de.redsix.pdfcompare.env.Environment;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.io.RandomAccess;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.io.RandomAccessStreamCache;
import org.apache.pdfbox.io.RandomAccessStreamCache.StreamCacheCreateFunction;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Utilities {

    private static final Logger LOG = LoggerFactory.getLogger(Utilities.class);

    public static StreamCacheCreateFunction getMemorySettings(final int bytes) throws IOException {
        return getMemorySettings(bytes, null);
    }

    /**
     * PDFBox memory settings that keep up to {@code bytes} in memory and spill to scratch files on disk.
     * Every document created with them gets its own temporary directory under {@code tempDirParent}
     * (java.io.tmpdir when it is null), which is removed when the document is closed.
     */
    public static StreamCacheCreateFunction getMemorySettings(final int bytes, final Path tempDirParent) throws IOException {
        probeTempDir(tempDirParent);
        return () -> {
            final Path tempDir = FileUtils.createTempDir(tempDirParent, "PdfBox");
            final RandomAccessStreamCache scratchFile = MemoryUsageSetting.setupMixed(bytes).setTempDir(tempDir.toFile()).streamCache.create();
            return new RandomAccessStreamCache() {

                @Override
                public RandomAccess createBuffer() throws IOException {
                    return scratchFile.createBuffer();
                }

                @Override
                public void close() throws IOException {
                    try {
                        scratchFile.close();
                    } finally {
                        FileUtils.removeTempDir(tempDir);
                    }
                }
            };
        };
    }

    // PDFBox swallows an IOException from the cache factory and falls back to an unbounded in-memory cache,
    // so an unusable temp dir has to fail here. Wrapping hides a NoSuchFileException from the
    // "input file missing" handling in PdfComparator.compare.
    private static void probeTempDir(final Path tempDirParent) throws IOException {
        try {
            FileUtils.removeTempDir(FileUtils.createTempDir(tempDirParent, "PdfBox"));
        } catch (NoSuchFileException e) {
            throw new IOException("Can not create a temporary directory, the parent directory does not exist: " + e.getFile(), e);
        }
    }

    static class NamedThreadFactory implements ThreadFactory {

        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        NamedThreadFactory(final String name) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = name + "-" + poolNumber.getAndIncrement() + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement());
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    public static ExecutorService blockingExecutor(final String name, int coreThreads, int maxThreads, int queueCapacity, Environment environment) {
        if (environment.useParallelProcessing()) {
            return new ThreadPoolExecutor(coreThreads, maxThreads, 3, TimeUnit.MINUTES,
                    new LinkedBlockingQueue<>(queueCapacity), new NamedThreadFactory(name), new BlockingHandler());
        } else {
            return new InThreadExecutorService();
        }
    }

    public static ExecutorService blockingExecutor(final String name, int threads, int queueCapacity, Environment environment) {
        if (environment.useParallelProcessing()) {
            return new ThreadPoolExecutor(threads, threads, 0, TimeUnit.MINUTES,
                    new LinkedBlockingQueue<>(queueCapacity), new NamedThreadFactory(name), new BlockingHandler());
        } else {
            return new InThreadExecutorService();
        }
    }

    public static void shutdownAndAwaitTermination(final ExecutorService executor, final String executorName, Environment environment) {
        shutdownAndAwaitTermination(executor, executorName, environment.getExecutorTimeout());
    }

    public static void shutdownAndAwaitTermination(final ExecutorService executor, final String executorName, int executorTimeoutInSeconds) {
        if (executor != null) {
            executor.shutdown();
            try {
                final TimeUnit unit = TimeUnit.SECONDS;
                if (!executor.awaitTermination(executorTimeoutInSeconds, unit)) {
                    LOG.error("Awaiting Shutdown of Executor '{}' timed out after {} {}", executorName, executorTimeoutInSeconds, unit);
                }
            } catch (InterruptedException e) {
                LOG.warn("Awaiting Shutdown of Executor '{}' was interrupted", executorName);
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void await(final CountDownLatch latch, final String latchName, Environment environment) {
        try {
            final int timeout = environment.getOverallTimeout();
            final TimeUnit unit = TimeUnit.MINUTES;
            if (!latch.await(timeout, unit)) {
                LOG.error("Awaiting Latch '{}' timed out after {} {}", latchName, timeout, unit);
            }
        } catch (InterruptedException e) {
            LOG.warn("Awaiting Latch '{}' was interrupted", latchName);
            Thread.currentThread().interrupt();
        }
    }

    public static int getNumberOfPages(final Path document, Environment environment) throws IOException {
        try (InputStream documentIS = Files.newInputStream(document)) {
            return getNumberOfPages(documentIS, environment);
        }
    }

    private static int getNumberOfPages(final InputStream documentIS, Environment environment) throws IOException {
        try (PDDocument pdDocument = Loader.loadPDF(new RandomAccessReadBuffer(documentIS), getMemorySettings(environment.getDocumentCacheSize(), environment.getTempDirectory()))) {
            return pdDocument.getNumberOfPages();
        }
    }

    public static ImageWithDimension renderPage(final Path document, final int page, Environment environment) throws IOException {
        try (InputStream documentIS = Files.newInputStream(document)) {
            return renderPage(documentIS, page, environment);
        }
    }

    public static ImageWithDimension renderPage(final InputStream documentIS, final int page, Environment environment) throws IOException {
        try (PDDocument pdDocument = Loader.loadPDF(new RandomAccessReadBuffer(documentIS), getMemorySettings(environment.getDocumentCacheSize(), environment.getTempDirectory()))) {
            if (page >= pdDocument.getNumberOfPages()) {
                throw new IllegalArgumentException("Page out of range. Last page is: " + pdDocument.getNumberOfPages());
            }
            pdDocument.setResourceCache(new ResourceCacheWithLimitedImages(environment));
            PDFRenderer pdfRenderer = new PDFRenderer(pdDocument);
            return PdfComparator.renderPageAsImage(pdDocument, pdfRenderer, page, environment);
        }
    }
}
