package de.redsix.pdfcompare;

/**
 * This CompareResult monitors the number of pages in the result.
 * When a threshold is reached, the pages are swapped to disk.
 */
public class CompareResultWithPageOverflow extends AbstractCompareResultWithSwap {

    private final int maxPages;

    public CompareResultWithPageOverflow() {
        this.maxPages = 10;
    }

    public CompareResultWithPageOverflow(final int maxPages) {
        this.maxPages = maxPages;
    }

    @Override
    protected boolean needToSwap() {
        return diffImages.size() >= maxPages;
    }
}
