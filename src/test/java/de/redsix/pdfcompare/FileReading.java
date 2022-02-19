package de.redsix.pdfcompare;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileReading {
    protected InputStream r(final String s) {
        return getClass().getResourceAsStream(s);
    }

    protected File f(final String s) {
        return new File(getClass().getResource(s).getFile());
    }

    protected Path p(final String s) {
        try {
            return Paths.get(getClass().getResource(s).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
