package de.redsix.pdfcompare;

public class PageArea {

    final int page;
    private final int x1;
    private final int y1;
    private final int x2;
    private final int y2;

    public PageArea(final int page) {
        this.page = page;
        this.x1 = -1;
        this.y1 = -1;
        this.x2 = -1;
        this.y2 = -1;
    }

    public PageArea(final int x1, final int y1, final int x2, final int y2) {
        checkCoordinates(x1, y1, x2, y2);
        this.page = -1;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public PageArea(final int page, final int x1, final int y1, final int x2, final int y2) {
        checkCoordinates(x1, y1, x2, y2);
        if (page < 1) {
            throw new IllegalArgumentException("Page has to be greater or equal to 1");
        }
        this.page = page;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    private void checkCoordinates(final int x1, final int y1, final int x2, final int y2) {
        if (x1 < 0 || x2 < 0 || y1 < 0 || y2 < 0) {
            throw new IllegalArgumentException("Coordinates have to be greater than 0");
        }
        if (x1 > x2 || y1 > y2) {
            throw new IllegalArgumentException("x1 has to be smaller or equal to x2 and y1 has to be smaller or equal to y2");
        }
    }

    public boolean contains(int x, int y) {
        if (x1 == -1 && y1 == -1 && x2 == -1 && y2 == -1) {
            return true;
        }
        return x >= x1 && x <= x2 && y >= y1 && y <= y2;
    }

    public int getPage() {
        return page;
    }

    public int getX1() {
        return x1;
    }

    public int getY1() {
        return y1;
    }

    public int getX2() {
        return x2;
    }

    public int getY2() {
        return y2;
    }
}
