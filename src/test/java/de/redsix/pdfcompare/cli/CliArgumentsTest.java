package de.redsix.pdfcompare.cli;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class CliArgumentsTest {

    @Test
    public void cliIsAvailableWhenExpectedAndActualFilenameAreProvided() {
        CliArguments cliArguments = new CliArguments(new String[]{"expected.pdf", "actual.pdf"});

        assertThat(cliArguments.hasFileArguments(), is(true));
    }

    @Test
    public void cliIsNotAvailableWhenOnlyOneFilenameIsProvided() {
        CliArguments cliArguments = new CliArguments(new String[]{"expected.pdf"});

        assertThat(cliArguments.hasFileArguments(), is(false));
    }

    @Test
    public void cliIsNotAvailableWhenMoreArgumentsAreProvidedThanExpected() {
        CliArguments cliArguments = new CliArguments(new String[]{"a.pdf", "b.pdf", "c.pdf"});

        assertThat(cliArguments.hasFileArguments(), is(false));
    }

    @Test
    public void cliIsNotAvailableWhenNoArgumentsAreProvided() {
        CliArguments cliArguments = new CliArguments(new String[]{});

        assertThat(cliArguments.hasFileArguments(), is(false));
    }

    @Test
    public void provideExpectedAndActualFilename() {
        CliArguments cliArguments = new CliArguments(new String[]{"expected.pdf", "actual.pdf"});

        assertThat(cliArguments.getExpectedFile().isPresent(), is(true));
        assertThat(cliArguments.getExpectedFile().get(), equalTo("expected.pdf"));
        assertThat(cliArguments.getActualFile().isPresent(), is(true));
        assertThat(cliArguments.getActualFile().get(), equalTo("actual.pdf"));
    }

    @Test
    public void provideOutputFilenameWithShortArgument() {
        CliArguments cliArguments = new CliArguments(new String[]{"-o", "result.pdf"});

        assertThat(cliArguments.getOutputFile().isPresent(), is(true));
        assertThat(cliArguments.getOutputFile().get(), equalTo("result.pdf"));
    }

    @Test
    public void provideExpectedPasswordWithShortArgument() {
        CliArguments cliArguments = new CliArguments(new String[]{"-exppwd", "MyPwd"});

        assertThat(cliArguments.getExpectedPassword().isPresent(), is(true));
        assertThat(cliArguments.getExpectedPassword().get(), equalTo("MyPwd"));
    }

    @Test
    public void provideOutputFilenameWithLongArgument() {
        CliArguments cliArguments = new CliArguments(new String[]{"--output", "result.pdf"});

        assertThat(cliArguments.getOutputFile().isPresent(), is(true));
        assertThat(cliArguments.getOutputFile().get(), equalTo("result.pdf"));
    }

    @Test
    public void cliWithoutArgumentsGivesError() {
        assertThat(new CliArguments(new String[]{}).execute(), equalTo(2));
    }

    @Test
    public void comparesTwoEqualFilesAndReturnsZero() {
        assertThat(new CliArguments(new String[]{
                "src/test/resources/de/redsix/pdfcompare/expectedSameAsActual.pdf",
                "src/test/resources/de/redsix/pdfcompare/actual.pdf"})
                .execute(), equalTo(0));
    }

    @Test
    public void comparesTwoDifferentFilesAndReturnsOne() {
        assertThat(new CliArguments(new String[]{
                "src/test/resources/de/redsix/pdfcompare/expected.pdf",
                "src/test/resources/de/redsix/pdfcompare/actual.pdf"})
                .execute(), equalTo(1));
    }

    @Test
    public void comparesTwoDifferentFilesWithExclusionReturnsZero() {
        assertThat(new CliArguments(new String[]{
                "src/test/resources/de/redsix/pdfcompare/expected.pdf",
                "src/test/resources/de/redsix/pdfcompare/actual.pdf",
                "-x", "src/test/resources/de/redsix/pdfcompare/ignore.conf"})
                .execute(), equalTo(0));
    }
}
