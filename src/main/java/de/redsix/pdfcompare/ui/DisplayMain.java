package de.redsix.pdfcompare.ui;

import de.redsix.pdfcompare.cli.CliArguments;
import de.redsix.pdfcompare.cli.CliArgumentsParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DisplayMain {

    private static final Logger LOG = LoggerFactory.getLogger(DisplayMain.class);

    public static void main(String[] args) {
        try {
            final CliArguments cliArguments = new CliArguments(args);

            final Display display = new Display();
            if (cliArguments.hasFileArguments()) {
                display.init(cliArguments);
            } else {
                display.init();
            }
        } catch (CliArgumentsParseException exception) {
            LOG.error(exception.getMessage());
        }
    }
}
