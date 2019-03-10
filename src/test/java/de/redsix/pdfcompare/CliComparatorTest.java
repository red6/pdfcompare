package de.redsix.pdfcompare;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import de.redsix.pdfcompare.cli.CliArgumentsImpl;
import de.redsix.pdfcompare.cli.CliComparator;
import org.junit.jupiter.api.Test;


public class CliComparatorTest {

    @Test
    public void comparesTwoEqualFilesAndReturnsZero() {
        CliComparator testCliComparator = new CliComparator(new CliArgumentsImpl(new String[]{"actual.pdf", "actual.pdf"}));

        assertThat(testCliComparator.getResult(), equalTo(0));
    }

    @Test
    public void comparesTwoDifferentFilesAndReturnsOne() {
        CliComparator testCliComparator = new CliComparator(new CliArgumentsImpl(new String[]{"expected.pdf", "actual.pdf"}));

        assertThat(testCliComparator.getResult(), equalTo(1));
    }

}
