package de.redsix.pdfcompare.cli;

import org.apache.commons.cli.*;

import java.util.Optional;

public class CliArgumentsImpl implements CliArguments {

    private static final int EXPECTED_FILENAME_INDEX = 0;
    private static final int ACTUAL_FILENAME_INDEX = 1;

    private static final String OUTPUT_OPTION = "o";
    private static final String OUTPUT_LONG_OPTION = "output";
    private static final String HELP_OPTION = "h";
    private static final String HELP_LONG_OPTION = "help";

    private final Options options;
    private CommandLine commandLine;

    public CliArgumentsImpl(String[] args) {
        options = new Options();
        options.addOption(buildOutputOption());
        options.addOption(buildHelpOption());

        process(args);
    }

    @Override
    public Boolean areAvailable() {
        return commandLine.getArgList().size() == 2 && getExpectedFile().isPresent() && getActualFile().isPresent();
    }

    @Override
    public Boolean isHelp() {
        return commandLine.hasOption(HELP_OPTION);
    }

    @Override
    public Optional<String> getExpectedFile() {
        return getRemainingArgument(EXPECTED_FILENAME_INDEX);
    }

    @Override
    public Optional<String> getActualFile() {
        return getRemainingArgument(ACTUAL_FILENAME_INDEX);
    }

    @Override
    public Optional<String> getOutputFile() {
        if (! commandLine.hasOption(OUTPUT_OPTION)) {
            return Optional.empty();
        }

        return Optional.of(commandLine.getOptionValue(OUTPUT_OPTION));
    }

    @Override
    public void printHelp() {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("java -jar pdfcompare-x.x.x.jar [EXPECTED] [ACTUAL]", options);
    }

    private Option buildOutputOption() {
        return Option.builder(OUTPUT_OPTION)
            .argName("output")
            .desc("Provide an optional output file for the result")
            .hasArg(true)
            .longOpt(OUTPUT_LONG_OPTION)
            .numberOfArgs(1)
            .required(false)
            .type(String.class)
            .valueSeparator('=')
            .build();
    }

    private Option buildHelpOption() {
        return Option.builder(HELP_OPTION)
                .argName("help")
                .desc("Displays this text and exit")
                .hasArg(false)
                .longOpt(HELP_LONG_OPTION)
                .numberOfArgs(0)
                .required(false)
                .build();
    }

    private void process(String[] args) {
        try {
            CommandLineParser commandLineParser = new DefaultParser();
            commandLine = commandLineParser.parse(options, args);
        } catch (ParseException exception) {
            throw new CliArgumentsParseException(exception);
        }
    }

    private Optional<String> getRemainingArgument(int index) {
        if (commandLine.getArgList().isEmpty() || commandLine.getArgList().size() < index + 1) {
            return Optional.empty();
        }

        return Optional.of(commandLine.getArgList().get(index));
    }
}
