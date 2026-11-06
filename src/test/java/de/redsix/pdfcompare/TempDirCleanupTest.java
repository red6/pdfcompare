package de.redsix.pdfcompare;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertThrows;

import de.redsix.pdfcompare.env.Environment;
import de.redsix.pdfcompare.env.SimpleEnvironment;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TempDirCleanupTest {

    @Test
    public void pdfBoxTempDirsAreRemovedWhenCompareReturns(@TempDir Path tempDir) throws IOException {
        final Set<String> seenWhileComparing = ConcurrentHashMap.newKeySet();
        final CompareResultImpl result = new CompareResultImpl() {
            @Override
            public synchronized void addPage(final PageDiffCalculator diffCalculator, final int pageIndex, final ImageWithDimension expectedImage,
                    final ImageWithDimension actualImage, final List<ImageWithDimension> resultPages) {
                seenWhileComparing.addAll(entries(tempDir));
                super.addPage(diffCalculator, pageIndex, expectedImage, actualImage, resultPages);
            }
        };

        new PdfComparator<>(pdf(null), pdf(null), result)
                .withEnvironment(new SimpleEnvironment().setTempDirectory(tempDir))
                .compare();

        assertThat(seenWhileComparing, hasItem(startsWith("PdfBox")));
        assertThat(entries(tempDir), empty());
    }

    @Test
    public void pdfBoxTempDirsAreRemovedWhenLoadingFails(@TempDir Path tempDir) throws IOException {
        final PdfComparator<CompareResultImpl> comparator = new PdfComparator<>(pdf("secret"), pdf("secret"))
                .withEnvironment(new SimpleEnvironment().setTempDirectory(tempDir))
                .withExpectedPassword("wrong");

        assertThrows(InvalidPasswordException.class, comparator::compare);
        assertThat(entries(tempDir), empty());
    }

    @Test
    public void unusableTempDirFailsLoudly(@TempDir Path tempDir) throws IOException {
        final Path missing = tempDir.resolve("does-not-exist");
        final PdfComparator<CompareResultImpl> comparator = new PdfComparator<>(pdf(null), pdf(null))
                .withEnvironment(new SimpleEnvironment().setTempDirectory(missing));

        final IOException e = assertThrows(IOException.class, comparator::compare);
        assertThat(e.getMessage(), containsString(missing.toString()));
    }

    @Test
    public void environmentWithoutTempDirectoryStillCompares() throws IOException {
        final Environment withoutTempDirectory = new SimpleEnvironment() {
            @Override
            public Path getTempDirectory() {
                return null;
            }
        };

        assertThat(new PdfComparator<>(pdf(null), pdf(null)).withEnvironment(withoutTempDirectory).compare().isEqual(), is(true));
    }

    private static List<String> entries(final Path dir) {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.map(p -> p.getFileName().toString()).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static InputStream pdf(final String password) throws IOException {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.A6));
            if (password != null) {
                document.protect(new StandardProtectionPolicy(password, password, new AccessPermission()));
            }
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
