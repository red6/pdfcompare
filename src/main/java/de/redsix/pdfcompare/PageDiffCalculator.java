package de.redsix.pdfcompare;

import java.util.Objects;

public class PageDiffCalculator {

    private final int totalPixels;
    private final double allowedDiffInPercent;
    private int diffsFound = 0;
    private int diffsFoundInExclusion = 0;
    private PageArea diffArea;

    public PageDiffCalculator(final int totalPixels, final double allowedDiffInPercent) {
        this.totalPixels = totalPixels;
        this.allowedDiffInPercent = allowedDiffInPercent;
    }

    /**
     * This is a convenience constructor for a single diff.
     * The result is the same as the following code:
     * <pre>{@code
     * pdc = new PageDiffCalculator(0, 0);
     * pdc.diffFound();
     * pdc.addDiffArea(pageArea);
     * }</pre>
     *
     * @param diffArea the page area that covers the diff.
     */
    public PageDiffCalculator(final PageArea diffArea) {
        totalPixels = 0;
        allowedDiffInPercent = 0;
        diffsFound = 1;
        this.diffArea = diffArea;
    }

    public void diffFound() {
        ++diffsFound;
    }

    public void diffFoundInExclusion() {
        ++diffsFoundInExclusion;
    }

    public boolean differencesFound() {
        double allowedDiffInPixels = totalPixels == 0 ? 0 : totalPixels * allowedDiffInPercent / 100.0;
        return diffsFound > allowedDiffInPixels;
    }

    public boolean differencesFoundInExclusion() {
        return diffsFoundInExclusion > 0;
    }

    public double getDifferenceInPercent() {
        if (totalPixels == 0) {
            return diffsFound > 0 ? 100.0 : 0.0;
        } else {
            return (double)diffsFound / (double)totalPixels * 100.0;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PageDiffCalculator)) {
            return false;
        }
        PageDiffCalculator that = (PageDiffCalculator) o;
        return diffsFound == that.diffsFound &&
                diffsFoundInExclusion == that.diffsFoundInExclusion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(diffsFound, diffsFoundInExclusion);
    }

    public void addDiffArea(final PageArea diffArea) {
        this.diffArea = diffArea;
    }

    public PageArea getDiffArea() {
        return diffArea;
    }
}
