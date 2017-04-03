package de.redsix.pdfcompare;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class BlockingHandler implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
        boolean success = false;
        do {
            try {
                if (!executor.isShutdown()) {
                    executor.getQueue().put(r);
                    success = true;
                }
            } catch (InterruptedException e) {
                // ignore and retry
            }
        }
        while (!success);
    }
}
