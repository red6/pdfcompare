package de.redsix.pdfcompare;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;

public interface CompareResult {

    /**
     * Write the result Pdf to a file. Warning: This will remove the diffImages from memory!
     * Writing can only be done once.
     * When no diff images are present, nothing is written.
     *
     * @param filename without pdf-Extension
     * @return a boolean indicating, whether the comparison is equal. When true, the files are equal.
     */
    boolean writeTo(String filename);

    /**
     * Write the result Pdf to an OutputStream. Warning: This will remove the diffImages from memory!
     * Writing can only be done once.
     * When no diff images are present, nothing is written.
     * The given OutputStream will be closed when this method returns.
     *
     * @param outputStream to write the pdfFile to.
     * @return a boolean indicating, whether the comparison is equal. When true, the files are equal.
     */
    boolean writeTo(OutputStream outputStream);

    /**
     * Returns, whether the compared documents are equal or not.
     * Documents are also equal, when differences are only in excluded areas.
     *
     * @return true, when no differences are found.
     */
    boolean isEqual();

    /**
     * The inverse of isEqual()
     *
     * @return true, when differences are found.
     */
    boolean isNotEqual();

    /**
     * Allows to check for differences in excluded areas.
     *
     * @return true, when there where differences in excluded areas.
     */
    boolean hasDifferenceInExclusion();

    /**
     * Only true, when there was no actual document at all.
     *
     * @return true, when the actual document was missing or not readable.
     */
    boolean hasOnlyExpected();

    /**
     * Only true, when there was no expected document at all.
     *
     * @return true, when the expected document was missing or not readable.
     */
    boolean hasOnlyActual();

    /**
     * Shows, whether one of the two document to comapre was missing or unreadable.
     *
     * @return true, when only an ectual or an expected document was found or readable, but not both.
     */
    boolean hasOnlyOneDoc();

    /**
     * Gives that total number of pages in the result. This includes pages with differences.
     * When the setting addEqualPagesToResult is set to true, the pages without differences are also counted.
     *
     * @return total number of pages in the result
     */
    int getNumberOfPages();

    /**
     * Gives a PageArea, that shows the area of a page, where differences where found.
     * There is only one PageArea per page, which contains all differences of the page.
     *
     * @return a collection of PageAreas, where differences where found.
     */
    Collection<PageArea> getDifferences();

    /**
     * Converts the result of the {@link CompareResult#getDifferences()} method to an exclusions JSON format.
     *
     * @return a JSON string with exclusion areas.
     */
    String getDifferencesJson();

    /**
     * Gives a collection of all the pages, that have a difference.
     * The first page of a document is page 1.
     *
     * @return collection of page numbers, that have a difference
     */
    Collection<Integer> getPagesWithDifferences();

    /**
     * Gives a map of the difference percentages per page.
     * 
     * @return difference percentages mapped to the page index.
     */
    Map<Integer, Double> getPageDiffsInPercent();
}
