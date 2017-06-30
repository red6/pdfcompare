package de.redsix.pdfcompare;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Paths;

import org.junit.Test;

public class ExclusionsTest {

    @Test
    public void readExclusions() {
        final Exclusions exclusions = new Exclusions();
        exclusions.readExclusions("ignore.conf");
        assertThat(exclusions.forPage(0).contains(300, 400), is(true));
        assertThat(exclusions.forPage(0).contains(600, 400), is(false));

        assertThat(exclusions.forPage(1).contains(1800, 250), is(true));
        assertThat(exclusions.forPage(1).contains(600, 400), is(false));

        assertThat(exclusions.forPage(2).contains(600, 400), is(false));
    }

    @Test
    public void readFromFile() {
        final Exclusions exclusions = new Exclusions();
        exclusions.readExclusions(new File("ignore.conf"));
        assertThat(exclusions.forPage(0).contains(300, 400), is(true));
    }

    @Test
    public void readFromPath() {
        final Exclusions exclusions = new Exclusions();
        exclusions.readExclusions(Paths.get("ignore.conf"));
        assertThat(exclusions.forPage(0).contains(300, 400), is(true));
    }

    @Test
    public void readFromInputStream() {
        final Exclusions exclusions = new Exclusions();
        exclusions.readExclusions(new ByteArrayInputStream("exclusions: [{x1: 230, y1: 350, x2: 450, y2: 420}]".getBytes()));
        assertThat(exclusions.forPage(0).contains(300, 400), is(true));
        assertThat(exclusions.forPage(7).contains(300, 400), is(true));
    }
}