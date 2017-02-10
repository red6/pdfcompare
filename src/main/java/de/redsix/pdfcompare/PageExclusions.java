package de.redsix.pdfcompare;

import java.util.ArrayList;
import java.util.Collection;

public class PageExclusions {

    private final Collection<Exclusion> exclusions = new ArrayList<>();

    public void add(final Exclusion exclusion) {
        exclusions.add(exclusion);
    }

    public boolean contains(final int x, final int y) {
        for (Exclusion exclusion : exclusions) {
            if (exclusion.contains(x, y)) {
                return true;
            }
        }
        return false;
    }
}
