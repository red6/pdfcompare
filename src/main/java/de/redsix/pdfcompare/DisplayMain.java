package de.redsix.pdfcompare;

import java.io.IOException;

public class DisplayMain {

    public static void main(String[] args) throws IOException {
        final CompareResult compareResult = new PdfComparator().compare("expected.pdf", "actual_marked.pdf");
        new Display(compareResult);
    }
}
