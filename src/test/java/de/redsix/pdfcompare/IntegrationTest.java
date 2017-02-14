package de.redsix.pdfcompare;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.junit.Test;

public class IntegrationTest {

    @Test
    public void differingDocumentsAreNotEqual() throws IOException {
        String file1="expected.pdf";
        String file2="actual.pdf";

        final CompareResult result = new PdfComparator(file1, file2).compare();
//        result.writeTo("differingDocumentsAreNotEqual");
        assertThat(result.isNotEqual(), is(true));
    }

    @Test
    public void differingDocumentsWithIgnoreAreEqual() throws IOException {
        String file1="expected.pdf";
        String file2="actual.pdf";

        final CompareResult result = new PdfComparator(file1, file2, "ignore.conf").compare();
//        result.writeTo("differingDocumentsWithIgnoreAreEqual");
        assertThat(result.isEqual(), is(true));
    }
}
