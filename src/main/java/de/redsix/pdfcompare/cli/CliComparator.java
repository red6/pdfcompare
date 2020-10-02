package de.redsix.pdfcompare.cli;

import de.redsix.pdfcompare.CompareResult;
import de.redsix.pdfcompare.CompareResultImpl;
import de.redsix.pdfcompare.PdfComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class CliComparator {

    private static final int EQUAL_DOCUMENTS_RESULT_VALUE = 0;
    private static final int UNEQUAL_DOCUMENTS_RESULT_VALUE = 1;
    private static final int ERROR_RESULT_VALUE = 2;

    private int result;
    private CompareResult compareResult;
    private static final Logger LOG = LoggerFactory.getLogger(CliComparator.class);

    public CliComparator(CliArguments cliArguments) {
        if (cliArguments.getExpectedFile().isPresent() && cliArguments.getActualFile().isPresent()) {
            result = compare(cliArguments.getExpectedFile().get(), cliArguments.getActualFile().get(), cliArguments.getExclusionsFile());

            if (cliArguments.getOutputFile().isPresent()) {
                compareResult.writeTo(cliArguments.getOutputFile().get());
            }
        }
    }

    public int getResult() {
        return result;
    }

    private int compare(String expectedFile, String actualFile, Optional<String> exclusions) {
        try {
            PdfComparator<CompareResultImpl> pdfComparator = new PdfComparator<>(expectedFile, actualFile);
            exclusions.ifPresent(x -> pdfComparator.withIgnore(x));
            compareResult = pdfComparator.compare();

            return (compareResult.isEqual()) ? EQUAL_DOCUMENTS_RESULT_VALUE : UNEQUAL_DOCUMENTS_RESULT_VALUE;
        } catch (IOException ex) {
            LOG.error(ex.getMessage());

            return ERROR_RESULT_VALUE;
        }
    }
}
