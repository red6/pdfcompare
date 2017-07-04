package de.redsix.pdfcompare;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Paths;

import org.junit.Test;

import com.typesafe.config.ConfigException;

public class ExclusionsTest {

    private final Exclusions exclusions = new Exclusions();

    @Test
    public void readExclusions() {
        exclusions.readExclusions("ignore.conf");
        assertThat(exclusions.forPage(0).contains(300, 400), is(true));
        assertThat(exclusions.forPage(0).contains(600, 400), is(false));

        assertThat(exclusions.forPage(1).contains(1800, 250), is(true));
        assertThat(exclusions.forPage(1).contains(600, 400), is(false));

        assertThat(exclusions.forPage(2).contains(600, 400), is(false));
    }

    @Test
    public void readFromFile() {
        exclusions.readExclusions(new File("ignore.conf"));
        assertThat(exclusions.forPage(0).contains(300, 400), is(true));
    }

    @Test
    public void readFromPath() {
        exclusions.readExclusions(Paths.get("ignore.conf"));
        assertThat(exclusions.forPage(0).contains(300, 400), is(true));
    }

    @Test
    public void readFromInputStreamWithoutPage() {
        exclusions.readExclusions(new ByteArrayInputStream("exclusions: [{x1: 230, y1: 350, x2: 450, y2: 420}]".getBytes()));
        assertThat(exclusions.forPage(0).contains(300, 400), is(true));
        assertThat(exclusions.forPage(7).contains(300, 400), is(true));
    }

    @Test
    public void readFromInputStreamPageOnly() {
        exclusions.readExclusions(new ByteArrayInputStream("exclusions: [{page: 3}]".getBytes()));
        assertThat(exclusions.forPage(0).contains(300, 400), is(false));
        assertThat(exclusions.forPage(2).contains(300, 400), is(true));
    }

    @Test(expected = ConfigException.class)
    public void missingCoordinateIsRejected() {
        exclusions.readExclusions(new ByteArrayInputStream("exclusions: [{page: 3, x1: 230, y1: 350, x2: 450, y3: 420}]".getBytes()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void coordinateBelowZeroAreRejected() {
        exclusions.readExclusions(new ByteArrayInputStream("exclusions: [{page: 3, x1: 230, y1: 350, x2: 450, y2: -1}]".getBytes()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void pageBelowOneAreRejected() {
        exclusions.readExclusions(new ByteArrayInputStream("exclusions: [{page: 0, x1: 230, y1: 350, x2: 450, y2: 600}]".getBytes()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongCoordinateOrderIsRejected() {
        exclusions.readExclusions(new ByteArrayInputStream("exclusions: [{page: 3, x1: 230, y1: 350, x2: 150, y2: 600}]".getBytes()));
    }
}