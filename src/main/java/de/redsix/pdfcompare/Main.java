package de.redsix.pdfcompare;

import de.redsix.pdfcompare.cli.CliArguments;
import de.redsix.pdfcompare.cli.CliArgumentsImpl;
import de.redsix.pdfcompare.cli.CliArgumentsParseException;
import de.redsix.pdfcompare.cli.CliComparator;
import de.redsix.pdfcompare.ui.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            final CliArguments cliArguments = new CliArgumentsImpl(args);

            if (cliArguments.areAvailable()) {
                System.exit(startCLI(cliArguments));
            } else if (cliArguments.isHelp()) {
                cliArguments.printHelp();
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

    private static int startCLI(CliArguments cliArguments) {
        return new CliComparator(cliArguments).getResult();
    }
}
