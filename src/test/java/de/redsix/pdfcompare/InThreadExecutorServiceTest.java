package de.redsix.pdfcompare;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

import de.redsix.pdfcompare.InThreadExecutorService.ImmediateFuture;

public class InThreadExecutorServiceTest {

    @Test
    public void run() throws ExecutionException, InterruptedException {
        final Future<String> future = new InThreadExecutorService().submit(() -> "Test");
        assertThat(future.get(), is("Test"));
    }

    @Test
    public void immediateFutureWithResult() throws ExecutionException, InterruptedException {
        final ImmediateFuture future = new ImmediateFuture<>("Test");
        assertThat(future.get(), is("Test"));
    }

    @Test
    public void immediateFutureWithException() throws ExecutionException, InterruptedException {
        final ImmediateFuture future = new ImmediateFuture(new Exception());
        assertThrows(Exception.class, () ->
                future.get());
    }
}
