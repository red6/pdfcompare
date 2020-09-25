package de.redsix.pdfcompare.ui;

import java.io.File;
import java.io.IOException;

public class DisplayMain {

    /**
     * You can specify the DPI for the XY Mode as system property 'DPI'. Default value is 300.
     * @param args You can specify 0 or 2 arguments. Specifying 2 args auto opens the files.
     * First argument: expected PDF file, 2nd argument: actual PDF file.
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Display d = new Display();
        String dpi = System.getProperty("DPI");
        if (dpi != null && dpi.length() > 0) {
            d.setDPI(Integer.parseInt(dpi));
            System.out.println("DPI: " + d.getDPI());
        }
        d.init();
        if (args.length >= 2) {
            File expected = new File(args[0]);
            File actual = new File(args[1]);
            d.openFiles(expected, actual, null, null);
        }
    }
}
