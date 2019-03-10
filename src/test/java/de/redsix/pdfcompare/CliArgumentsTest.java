package de.redsix.pdfcompare;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;

import de.redsix.pdfcompare.cli.CliArguments;
import de.redsix.pdfcompare.cli.CliArgumentsImpl;
import org.junit.jupiter.api.Test;

public class CliArgumentsTest {

    @Test
    public void cliIsAvailableWhenExpectedAndActualFilenameAreProvided() {
        CliArguments cliArguments = new CliArgumentsImpl(new String[]{"expected.pdf", "actual.pdf"});

        assertThat(cliArguments.areAvailable(), is(true));
    }

    @Test
    public void cliIsNotAvailableWhenOnlyOneFilenameIsProvided() {
        CliArguments cliArguments = new CliArgumentsImpl(new String[]{"expected.pdf"});

        assertThat(cliArguments.areAvailable(), is(false));
    }

    @Test
    public void cliIsNotAvailableWhenMoreArgumentsAreProvidedThanExpected() {
        CliArguments cliArguments = new CliArgumentsImpl(new String[]{"a.pdf", "b.pdf", "c.pdf"});

        assertThat(cliArguments.areAvailable(), is(false));
    }

    @Test
    public void cliIsNotAvailableWhenNoArgumentsAreProvided() {
        CliArguments cliArguments = new CliArgumentsImpl(new String[]{});

        assertThat(cliArguments.areAvailable(), is(false));
    }

    @Test
    public void provideExpectedAndActualFilename() {
        CliArguments cliArguments = new CliArgumentsImpl(new String[]{"expected.pdf", "actual.pdf"});

        assertThat(cliArguments.getExpectedFile().isPresent(), is(true));
        assertThat(cliArguments.getExpectedFile().get(), equalTo("expected.pdf"));
        assertThat(cliArguments.getActualFile().isPresent(), is(true));
        assertThat(cliArguments.getActualFile().get(), equalTo("actual.pdf"));
    }

    @Test
    public void provideOutputFilenameWithShortArgument() {
        CliArguments cliArguments = new CliArgumentsImpl(new String[]{"-o", "result.pdf"});

        assertThat(cliArguments.getOutputFile().isPresent(), is(true));
        assertThat(cliArguments.getOutputFile().get(), equalTo("result.pdf"));
    }

    @Test
    public void provideOutputFilenameWithLongArgument() {
        CliArguments cliArguments = new CliArgumentsImpl(new String[]{"--output", "result.pdf"});

        assertThat(cliArguments.getOutputFile().isPresent(), is(true));
        assertThat(cliArguments.getOutputFile().get(), equalTo("result.pdf"));
    }

}
