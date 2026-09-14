package de.redsix.pdfcompare;

import de.redsix.junitextensions.TempDirectory;
import de.redsix.junitextensions.TempDirectoryExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(TempDirectoryExtension.class)
public class FileReading {
    protected InputStream r(final String s) {
        return getClass().getResourceAsStream(s);
    }

    protected File f(final String s) {
        return new File(getClass().getResource(s).getFile());
    }

    protected Path p(final String s) {
        try {
            return Paths.get(getClass().getResource(s).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private String testName;
    private Path outDir;

    @BeforeEach
    public void before(TestInfo testInfo, @TempDirectory(parentPath = ".") Path outDir) {
        testName = testInfo.getTestMethod().get().getName();
        this.outDir = outDir;
    }

    protected Path resolve(final String filename) {
        return outDir.resolve(filename);
    }

    protected void writeAndCompare(final CompareResult result) throws IOException {
        if (System.getenv().get("pdfCompareInTest") != null || System.getProperty("pdfCompareInTest") != null) {
            final String filename = resolve(testName).toString();
            System.out.println("Writing file to: " + filename);
            result.writeTo(filename);
            try (final InputStream expectedPdf = getClass().getResourceAsStream(testName + ".pdf")) {
                if (expectedPdf != null) {
                    compareAndCheck(expectedPdf, filename, testName);
                } else {
                    assertFalse(Files.exists(Paths.get(filename + ".pdf")));
                }
            }
        }
    }

    protected void writeAndCompareWithOutputStream(final CompareResult result) throws IOException {
        if (System.getenv().get("pdfCompareInTest") != null || System.getProperty("pdfCompareInTest") != null) {
            final String filename = resolve(testName).toString();
            result.writeTo(new FileOutputStream(filename + ".pdf"));
            try (final InputStream expectedPdf = getClass().getResourceAsStream(testName + ".pdf")) {
                if (expectedPdf != null) {
                    compareAndCheck(expectedPdf, filename, testName);
                } else {
                    assertThat(Files.size(Paths.get(filename + ".pdf")), is(0));
                }
            }
        }
    }

    protected void compareAndCheck(InputStream expectedPdf, String filename, String testName) throws IOException {
        CompareResultImpl compare = new PdfComparator<>(expectedPdf, new FileInputStream(filename + ".pdf")).compare();
        if (!compare.isEqual()) {
            Files.move(Paths.get(filename + ".pdf"), Paths.get("actual_" + testName + ".pdf"), StandardCopyOption.REPLACE_EXISTING);
            compare.writeTo("diff_" + testName);
        }
        assertTrue(compare.isEqual());
    }
}
