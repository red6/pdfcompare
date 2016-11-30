package de.redsix.pdfcompare;

import java.io.IOException;

public class DisplayMain {

    public static void main(String[] args) throws IOException {
        final CompareResult compareResult = new PdfComparator().compare("expected.pdf", "actual_marked.pdf");
//        final CompareResult compareResult = new PdfComparator().compare("expected.pdf", "actual.pdf");
        //        final CompareResult compareResult = new PdfComparator().compare("expected.pdf", "PdfMitBild.pdf");
        //        final CompareResult compareResult = new PdfComparator().compare("PdfMitBild.pdf", "expected.pdf");
        new Display(compareResult);
    }
}
