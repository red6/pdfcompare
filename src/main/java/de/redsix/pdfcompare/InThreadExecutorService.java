package de.redsix.pdfcompare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class InThreadExecutorService implements ExecutorService {

    private boolean shutdown = false;

    @Override
    public void shutdown() {
        shutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown = true;
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isTerminated() {
        return shutdown;
    }

    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return true;
    }

    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        try {
            return new ImmediateFuture<>(task.call());
        } catch (Exception e) {
            return new ImmediateFuture<>(e);
        }
    }

    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        try {
            task.run();
        } catch (Exception e) {
            return new ImmediateFuture<>(e);
        }
        return new ImmediateFuture<>(result);
    }

    @Override
    public Future<?> submit(final Runnable task) {
        return submit(task, null);
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
        List<Future<T>> result = new ArrayList<>();
        for (Callable<T> task : tasks) {
            result.add(submit(task));
        }
        return result;
    }

    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
            throws InterruptedException {
        return invokeAll(tasks);
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return invokeAny(tasks);
    }

    @Override
    public void execute(final Runnable command) {
        command.run();
    }

    /*package for Testing*/ static class ImmediateFuture<T> implements Future<T> {

        private final T result;
        private final Exception exception;

        public ImmediateFuture(final T result) {
            this.result = result;
            this.exception = null;
        }

        public ImmediateFuture(final Exception exeption) {
            this.result = null;
            this.exception = exeption;
        }

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            return true;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            if (result != null) {
                return result;
            } else {
                throw new ExecutionException(exception);
            }
        }

        @Override
        public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return get();
        }
    }
}
