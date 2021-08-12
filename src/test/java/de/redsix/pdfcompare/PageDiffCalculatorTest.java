package de.redsix.pdfcompare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PageDiffCalculatorTest {

    @Test
    public void noDiffFound() {
        final PageDiffCalculator diffCalculator = new PageDiffCalculator(1000, 0);
        assertFalse(diffCalculator.differencesFound());
        assertFalse(diffCalculator.differencesFoundInExclusion());
    }

    @Test
    public void diffFoundInExclusionIsAlwaysCounted() {
        final PageDiffCalculator diffCalculator = new PageDiffCalculator(1000, 1);
        diffCalculator.diffFoundInExclusion();
        assertFalse(diffCalculator.differencesFound());
        assertTrue(diffCalculator.differencesFoundInExclusion());
    }

    @Test
    public void diffFoundWithZeroPercentAllowedIsCounted() {
        final PageDiffCalculator diffCalculator = new PageDiffCalculator(1000, 0);
        diffCalculator.diffFound();
        assertTrue(diffCalculator.differencesFound());
    }

    @Test
    public void diffBelowPercentageIsNotReportedAsDifferenceFound() {
        final PageDiffCalculator diffCalculator = new PageDiffCalculator(200, 1);
        diffCalculator.diffFound();
        diffCalculator.diffFound();
        assertFalse(diffCalculator.differencesFound());
    }

    @Test
    public void diffAbovePercentageIsReportedAsDifferenceFound() {
        final PageDiffCalculator diffCalculator = new PageDiffCalculator(200, 1);
        diffCalculator.diffFound();
        diffCalculator.diffFound();
        diffCalculator.diffFound();
        assertTrue(diffCalculator.differencesFound());
    }

    @Test
    public void diffBelowPercentageAsFractionIsNotSupported() {
        final PageDiffCalculator diffCalculator = new PageDiffCalculator(1000, 0.2);
        diffCalculator.diffFound();
        diffCalculator.diffFound();
        assertFalse(diffCalculator.differencesFound());
    }

    @Test
    public void diffAbovePercentageAsFractionIsReported() {
        final PageDiffCalculator diffCalculator = new PageDiffCalculator(1000, 0.2);
        diffCalculator.diffFound();
        diffCalculator.diffFound();
        diffCalculator.diffFound();
        assertTrue(diffCalculator.differencesFound());
    }

    @Test
    public void zeroTotalPixelsWithDiffFoundReportsHundredPercentDifference() {
        final PageDiffCalculator diffCalculator = new PageDiffCalculator(0, 0);
        diffCalculator.diffFound();
        assertEquals(100.0, diffCalculator.getDifferenceInPercent());
    }

    @Test
    public void zeroTotalPixelsWithNoDiffFoundReportsZeroPercentDifference() {
        final PageDiffCalculator diffCalculator = new PageDiffCalculator(0, 0);
        assertEquals(0.0, diffCalculator.getDifferenceInPercent());
    }

    @Test
    public void diffsFoundReportsCorrectPercentage() {
        final PageDiffCalculator diffCalculator = new PageDiffCalculator(10, 0);
        diffCalculator.diffFound();
        diffCalculator.diffFound();
        assertEquals(20.0, diffCalculator.getDifferenceInPercent());
    }

    @Test
    public void noDiffsFoundReportsCorrectPercentage() {
        final PageDiffCalculator diffCalculator = new PageDiffCalculator(6, 0);
        assertEquals(0.0, diffCalculator.getDifferenceInPercent());
    }
}