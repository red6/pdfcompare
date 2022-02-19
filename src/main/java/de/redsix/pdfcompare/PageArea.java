package de.redsix.pdfcompare;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Describes a rectangular area of a page or multiple pages.
 * Is is used to specify exclusions and areas, that differ.
 */
public class PageArea {

    final int page;
    private final int x1;
    private final int y1;
    private final int x2;
    private final int y2;

    /**
     * Defines the area for the whole page.
     *
     * @param page Page number starting with 1
     */
    public PageArea(final int page) {
        this.page = page;
        this.x1 = -1;
        this.y1 = -1;
        this.x2 = -1;
        this.y2 = -1;
    }

    /**
     * Defines the same area for every page.
     *
     * @param x1 x-coordinate of the upper left corner of the rectangle
     * @param y1 y-coordinate of the upper left corner of the rectangle
     * @param x2 x-coordinate of the lower right corner of the rectangle
     * @param y2 y-coordinate of the lower right corner of the rectangle
     */
    public PageArea(final int x1, final int y1, final int x2, final int y2) {
        checkCoordinates(x1, y1, x2, y2);
        this.page = -1;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    /**
     * Defines an area for one particular page.
     *
     * @param page Page number starting with 1
     * @param x1   x-coordinate of the upper left corner of the rectangle
     * @param y1   y-coordinate of the upper left corner of the rectangle
     * @param x2   x-coordinate of the lower right corner of the rectangle
     * @param y2   y-coordinate of the lower right corner of the rectangle
     */
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

    public boolean hasPage() {
        return page >= 1;
    }

    public boolean hasCoordinates() {
        return x1 >= 0;
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
    
    @Override
    public int hashCode() {
        return page + 31 * x1;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        
        if (obj == null || ! (obj instanceof PageArea)) {
            return false;
        }
        
        PageArea pageArea = (PageArea) obj;
        
        return this.getPage() == pageArea.getPage()
                && this.getX1() == pageArea.getX1()
                && this.getY1() == pageArea.getY1()
                && this.getX2() == pageArea.getX2()
                && this.getY2() == pageArea.getY2()
            ;
    }

    public String asJson() {
        if (hasPage()) {
            if (hasCoordinates()) {
                return "{\"page\": " + page + ", \"x1\": " + x1 + ", \"y1\": " + y1 + ", \"x2\": " + x2 + ", \"y2\": " + y2 + "}";
            } else {
                return "{\"page\": " + page + "}";
            }
        } else {
            return "{\"x1\": " + x1 + ", \"y1\": " + y1 + ", \"x2\": " + x2 + ", \"y2\": " + y2 + "}";
        }
    }

    public static String asJsonWithExclusion(Collection<PageArea> pageAreas) {
        return asJsonWithExclusion(pageAreas.stream());
    }

    public static String asJsonWithExclusion(Stream<PageArea> pageAreaStream) {
        String json = asJson(pageAreaStream);
        return json.isEmpty()
                ? "exclusions: [\n]"
                : "exclusions: [\n" + json + "\n]";
    }

    public static String asJson(Collection<PageArea> pageAreas) {
        return asJson(pageAreas.stream());
    }

    public static String asJson(Stream<PageArea> pageAreas) {
        return pageAreas.map(PageArea::asJson).collect(Collectors.joining(",\n"));
    }
}
