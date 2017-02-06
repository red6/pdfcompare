package de.redsix.pdfcompare;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;

public class Exclusions {

    private final Collection<Exclusion> exclusions = new ArrayList<>();

    public Exclusions() {}

    public void add(final Exclusion exclusion) {
        exclusions.add(exclusion);
    }

    public boolean contains(final int page, final int x, final int y) {
        return exclusions.stream().filter(e -> e.contains(page, x, y)).findFirst().isPresent();
    }

    public void readExclusions() {
        final Config load = ConfigFactory.parseFile(new File("ignore.conf"));
        final List<? extends ConfigObject> exclusions = load.getObjectList("ex");
        exclusions.stream().map(co -> {
            final Config c = co.toConfig();
            return new Exclusion(c.getInt("page"), c.getInt("x1"), c.getInt("y1"), c.getInt("x2"), c.getInt("y2"));
        }).forEach(e -> this.exclusions.add(e));
    }
}
