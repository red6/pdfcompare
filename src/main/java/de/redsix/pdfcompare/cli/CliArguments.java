package de.redsix.pdfcompare.cli;

import java.util.Optional;

public interface CliArguments {

    Boolean areAvailable();

    Boolean isHelp();

    Optional<String> getExpectedFile();

    Optional<String> getActualFile();

    Optional<String> getOutputFile();

    void printHelp();

}
