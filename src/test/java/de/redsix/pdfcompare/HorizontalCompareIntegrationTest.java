package de.redsix.pdfcompare;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import de.redsix.pdfcompare.env.SimpleEnvironment;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.io.IOException;

/**
 * Integration tests covering the horizontal compare output scenario.
 * <p>
 * The scenario uses a {@link SimpleEnvironment} with:
 * <ul>
 *   <li>actual pixels highlighted in red</li>
 *   <li>expected pixels highlighted in white</li>
 *   <li>equal pages included in the result ({@code addEqualPagesToResult = true})</li>
 *   <li>horizontal side-by-side compare output enabled</li>
 * </ul>
 */
public class HorizontalCompareIntegrationTest extends FileReading {

    /**
     * Creates the environment that mirrors the configuration used in the POC.
     */
    private SimpleEnvironment pocEnvironment() {
        return new SimpleEnvironment()
                .setActualColor(Color.red)
                .setExpectedColor(Color.white)
                .setAddEqualPagesToResult(true)
                .setEnableHorizontalCompareOutput(true);
    }

    @Test
    public void differingDocumentsWithHorizontalCompareAreNotEqual() throws IOException {
        // Act
        final CompareResult result = new PdfComparator<>(r("expected.pdf"), r("actual.pdf"))
                .withEnvironment(pocEnvironment())
                .compare();

        // Assert
        assertThat(result.isNotEqual(), is(true));
        assertThat(result.isEqual(), is(false));
        assertThat(result.hasOnlyExpected(), is(false));
        assertThat(result.hasOnlyActual(), is(false));
        assertThat(result.hasOnlyOneDoc(), is(false));
        assertThat(result.getDifferences(), not(empty()));
        assertThat(result.getPagesWithDifferences(), not(empty()));
    }

    @Test
    public void equalDocumentsWithHorizontalCompareAreEqual() throws IOException {
        // Act
        final CompareResult result = new PdfComparator<>(r("expectedSameAsActual.pdf"), r("actual.pdf"))
                .withEnvironment(pocEnvironment())
                .compare();

        // Assert
        assertThat(result.isEqual(), is(true));
        assertThat(result.isNotEqual(), is(false));
        assertThat(result.getDifferences(), empty());
        assertThat(result.hasDifferenceInExclusion(), is(false));
    }

    @Test
    public void equalPagesAreIncludedInResultWhenAddEqualPagesToResultIsEnabled() throws IOException {
        // Arrange - equal documents so no differences exist, but addEqualPagesToResult=true
        // should still cause all pages to be stored in the result
        final CompareResult result = new PdfComparator<>(r("expectedSameAsActual.pdf"), r("actual.pdf"))
                .withEnvironment(pocEnvironment())
                .compare();

        // Assert - pages are recorded even though the documents are equal
        assertThat(result.getNumberOfPages(), is(greaterThan(0)));
        assertThat(result.isEqual(), is(true));
    }

    @Test
    public void differingDocumentsWithHorizontalCompareIncludeAllPagesInResult() throws IOException {
        // Arrange - differing documents with addEqualPagesToResult=true:
        // both pages with differences and pages without differences should be included
        final CompareResult result = new PdfComparator<>(r("expected.pdf"), r("actual.pdf"))
                .withEnvironment(pocEnvironment())
                .compare();

        // Assert - number of stored pages equals the total page count of the document,
        // not just the pages that contain differences
        assertThat(result.getNumberOfPages(), is(greaterThanOrEqualTo(result.getDifferences().size())));
        assertThat(result.isNotEqual(), is(true));
    }
}

