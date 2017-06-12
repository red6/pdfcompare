package de.redsix.pdfcompare;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Test;

import de.redsix.pdfcompare.InThreadExecutorService.ImmediateFuture;

public class InThreadExecutorServiceTest {

    @Test
    public void run() throws ExecutionException, InterruptedException {
        final Future<String> future = new InThreadExecutorService().submit(() -> "Test");
        assertThat(future.get(), is("Test"));
    }

    @Test
    public void immediateFutureWithResult() throws ExecutionException, InterruptedException {
        final ImmediateFuture future = new ImmediateFuture("Test");
        assertThat(future.get(), is("Test"));
    }

    @Test(expected = Exception.class)
    public void immediateFutureWithException() throws ExecutionException, InterruptedException {
        final ImmediateFuture future = new ImmediateFuture(new Exception());
        future.get();
    }
}