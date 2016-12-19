package de.redsix.pdfcompare.ui;

import java.io.IOException;

import de.redsix.pdfcompare.CompareResult;

public class DisplayMain {

    public static void main(String[] args) throws IOException {
        new Display(new CompareResult());
    }
}
