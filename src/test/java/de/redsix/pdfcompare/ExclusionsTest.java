package de.redsix.pdfcompare;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.typesafe.config.ConfigException;
import de.redsix.pdfcompare.env.DefaultEnvironment;
import de.redsix.pdfcompare.env.SimpleEnvironment;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Paths;

public class ExclusionsTest extends FileReading {

    private final Exclusions exclusions = new Exclusions(DefaultEnvironment.create());

    @Test
    public void readExclusions() {
        exclusions.readExclusions("src/test/resources/de/redsix/pdfcompare/ignore.conf");
        assertThat(exclusions.forPage(1).contains(300, 400), is(true));
        assertThat(exclusions.forPage(1).contains(600, 400), is(false));

        assertThat(exclusions.forPage(2).contains(1800, 250), is(true));
        assertThat(exclusions.forPage(2).contains(600, 400), is(false));

        assertThat(exclusions.forPage(3).contains(600, 400), is(false));
    }

    @Test
    public void readFromFile() {
        exclusions.readExclusions(f("ignore.conf"));
        assertThat(exclusions.forPage(1).contains(300, 400), is(true));
        assertThat(exclusions.asJson(), is("exclusions: [\n" +
                "{\"page\": 1, \"x1\": 230, \"y1\": 350, \"x2\": 450, \"y2\": 420},\n" +
                "{\"page\": 2, \"x1\": 1750, \"y1\": 240, \"x2\": 2000, \"y2\": 300}\n" +
                "]"));
    }

    @Test
    public void readFromPath() {
        exclusions.readExclusions(p("ignore.conf"));
        assertThat(exclusions.forPage(1).contains(300, 400), is(true));
    }

    @Test
    public void missingPathIsIgnored() {
        exclusions.readExclusions(Paths.get("fileDoesNotExist.conf"));
        // No exclusions are read
        assertThat(exclusions.asJson(), is("exclusions: [\n]"));
    }

    @Test
    public void missingFileIsIgnored() {
        exclusions.readExclusions(new File("fileDoesNotExist.conf"));
        // No exclusions are read
        assertThat(exclusions.asJson(), is("exclusions: [\n]"));
    }

    @Test
    public void missingFileThrowsException() {
        Exclusions exclusions = new Exclusions(new SimpleEnvironment().setFailOnMissingIgnoreFile(true));
        assertThrows(IgnoreFileMissing.class,
                () -> exclusions.readExclusions(new File("fileDoesNotExist.conf")));
    }

    @Test
    public void readFromInputStreamWithoutPage() {
        exclusions.readExclusions(new ByteArrayInputStream("exclusions: [{x1: 230, y1: 350, x2: 450, y2: 420}]".getBytes()));
        assertThat(exclusions.forPage(1).contains(300, 400), is(true));
        assertThat(exclusions.forPage(8).contains(300, 400), is(true));
        assertThat(exclusions.asJson(), is("exclusions: [\n{\"x1\": 230, \"y1\": 350, \"x2\": 450, \"y2\": 420}\n]"));
    }

    @Test
    public void readFromInputStreamPageOnly() {
        exclusions.readExclusions(new ByteArrayInputStream("exclusions: [{page: 3}]".getBytes()));
        assertThat(exclusions.forPage(1).contains(300, 400), is(false));
        assertThat(exclusions.forPage(3).contains(300, 400), is(true));
        assertThat(exclusions.asJson(), is("exclusions: [\n{\"page\": 3}\n]"));
    }

    @Test
    public void withAndWithoutPageGetsSortedWithoutPageFirst() {
        exclusions.readExclusions(new ByteArrayInputStream(("exclusions: [" +
                "{page: 4, x1: 230, y1: 350, x2: 450, y2: 420}," +
                "{page: 2, x1: 230, y1: 350, x2: 450, y2: 420}," +
                "{page: 3}," +
                "{x1: 230, y1: 350, x2: 450, y2: 420}]").getBytes()));
        assertThat(exclusions.asJson(), is("exclusions: [\n" +
                "{\"x1\": 230, \"y1\": 350, \"x2\": 450, \"y2\": 420},\n" +
                "{\"page\": 2, \"x1\": 230, \"y1\": 350, \"x2\": 450, \"y2\": 420},\n" +
                "{\"page\": 3},\n" +
                "{\"page\": 4, \"x1\": 230, \"y1\": 350, \"x2\": 450, \"y2\": 420}\n" +
                "]"));
    }

    @Test
    public void missingCoordinateIsRejected() {
        assertThrows(ConfigException.class, () ->
                exclusions.readExclusions(new ByteArrayInputStream("exclusions: [{page: 3, x1: 230, y1: 350, x2: 450, y3: 420}]".getBytes())));
    }

    @Test
    public void coordinateBelowZeroAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                exclusions.readExclusions(new ByteArrayInputStream("exclusions: [{page: 3, x1: 230, y1: 350, x2: 450, y2: -1}]".getBytes())));
    }

    @Test
    public void pageBelowOneAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                exclusions.readExclusions(new ByteArrayInputStream("exclusions: [{page: 0, x1: 230, y1: 350, x2: 450, y2: 600}]".getBytes())));
    }

    @Test
    public void wrongCoordinateOrderIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                exclusions.readExclusions(new ByteArrayInputStream("exclusions: [{page: 3, x1: 230, y1: 350, x2: 150, y2: 600}]".getBytes())));
    }

    @Test
    public void coordinatesInCmAndMm() {
        exclusions.readExclusions(new ByteArrayInputStream("exclusions: [{page: 3, x1: 21mm, y1: 134mm, x2: 2.4cm, y2: 14cm}]".getBytes()));
        assertThat(exclusions.forPage(3).contains(247, 1583), is(false));
        assertThat(exclusions.forPage(3).contains(248, 1582), is(false));
        assertThat(exclusions.forPage(3).contains(248, 1583), is(true));
        assertThat(exclusions.forPage(3).contains(283, 1654), is(true));
        assertThat(exclusions.forPage(3).contains(284, 1654), is(false));
        assertThat(exclusions.forPage(3).contains(283, 1655), is(false));
    }

    @Test
    public void coordinatesInPt() {
        exclusions.readExclusions(new ByteArrayInputStream("exclusions: [{page: 3, x1: 21pt, y1: 1, x2: 30pt, y2: 10}]".getBytes()));
        assertThat(exclusions.forPage(3).contains(87, 1), is(false));
        assertThat(exclusions.forPage(3).contains(88, 1), is(true));
        assertThat(exclusions.forPage(3).contains(125, 1), is(true));
        assertThat(exclusions.forPage(3).contains(126, 1), is(false));
    }
}