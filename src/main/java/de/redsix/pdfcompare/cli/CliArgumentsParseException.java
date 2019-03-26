package de.redsix.pdfcompare.cli;

public class CliArgumentsParseException extends RuntimeException {

    public CliArgumentsParseException(Exception cause) {
        super("Failed processing Command Line Arguments.", cause);
    }

}
