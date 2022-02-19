package de.redsix.pdfcompare;

import de.redsix.pdfcompare.cli.CliArguments;
import de.redsix.pdfcompare.cli.CliArgumentsParseException;
import de.redsix.pdfcompare.ui.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            final CliArguments cliArguments = new CliArguments(args);

            if (args.length > 0) {
                System.exit(cliArguments.execute());
            } else {
                startUI();
            }
        } catch (CliArgumentsParseException exception) {
            LOG.error(exception.getMessage());
        }
    }

    private static void startUI() {
        new Display().init();
    }
}
