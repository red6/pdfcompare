package de.redsix.pdfcompare;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}