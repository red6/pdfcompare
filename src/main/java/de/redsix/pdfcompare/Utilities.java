package de.redsix.pdfcompare;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utilities {

    private static final Logger LOG = LoggerFactory.getLogger(Utilities.class);

    public static void shutdownAndAwaitTermination(final ExecutorService executor, final String executorName) {
        executor.shutdown();
        try {
            executor.awaitTermination(20, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LOG.warn("Awaiting Shutdown of Executor '{}' was interrupted", executorName);
        }
    }

    public static void await(final CountDownLatch latch, final String latchName) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            LOG.warn("Awaiting Latch '{}' was interrupted", latchName);
        }
    }
}
