package de.redsix.pdfcompare;

/**
 * This CompareResult monitors the number of pages in the result.
 * When a threshold is reached, the pages are swapped to disk.
 */
public class CompareResultWithPageOverflow extends AbstractCompareResultWithSwap {

    private final int maxPages;

    /**
     * Defaults to swap to disk, when more than 10 pages are stored.
     */
    public CompareResultWithPageOverflow() {
        this.maxPages = 10;
    }

    /**
     * Swaps to disk, when more than the given pages are in memory.
     * @param maxPages the maximum amount of pages to keep in memory
     */
    public CompareResultWithPageOverflow(final int maxPages) {
        this.maxPages = maxPages;
    }

    @Override
    protected synchronized boolean needToSwap() {
        return diffImages.size() >= maxPages;
    }
}
