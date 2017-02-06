package de.redsix.pdfcompare;

public class Exclusion {

    private final int page;
    private final int x1;
    private final int y1;
    private final int x2;
    private final int y2;

    public Exclusion(final int page, final int x1, final int y1, final int x2, final int y2) {
        this.page = page - 1;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public boolean contains(int page, int x, int y) {
        return page == this.page && x >= x1 && x <= x2 && y >= y1 && y <= y2;
    }
}
