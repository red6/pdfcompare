package de.redsix.pdfcompare;

import java.io.IOException;

public class DisplayMain {

    public static void main(String[] args) throws IOException {
        new Display(new CompareResult());
    }
}
