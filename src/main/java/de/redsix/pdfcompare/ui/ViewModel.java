package de.redsix.pdfcompare.ui;

import java.awt.image.BufferedImage;

import de.redsix.pdfcompare.CompareResultWithExpectedAndActual;

public class ViewModel {

    private final CompareResultWithExpectedAndActual result;
    private int pageToShow = 0;
    private boolean showExpected = true;
    private final int maxPages;

    public ViewModel(final CompareResultWithExpectedAndActual compareResult) {
        this.maxPages = compareResult.getNumberOfPages();
        this.result = compareResult;
    }

    public int getPageToShow() {
        return pageToShow;
    }

    public boolean isShowExpected() {
        return showExpected;
    }

    public void showExpected() {
        showExpected = true;
    }

    public void showActual() {
        showExpected = false;
    }

    public boolean decreasePage() {
        if (pageToShow > 0) {
            --pageToShow;
            return true;
        }
        return false;
    }

    public boolean increasePage() {
        if (pageToShow < maxPages) {
            ++pageToShow;
            return true;
        }
        return false;
    }

    public BufferedImage getLeftImage() {
        if (isShowExpected()) {
            return result.getExpectedImage(getPageToShow());
        } else {
            return result.getActualImage(getPageToShow());
        }
    }

    public BufferedImage getDiffImage() {
        return result.getDiffImage(getPageToShow());
    }
}
