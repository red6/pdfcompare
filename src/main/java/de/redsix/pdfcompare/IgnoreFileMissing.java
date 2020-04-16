package de.redsix.pdfcompare;

import java.io.File;

public class IgnoreFileMissing extends RuntimeException {
    private final File missingIgnoreFile;

    public IgnoreFileMissing(File missingIgnoreFile) {
        super("Ignore-file at '" + missingIgnoreFile + "' not found.");
        this.missingIgnoreFile = missingIgnoreFile;
    }

    public IgnoreFileMissing(File missingIgnoreFile, Throwable throwable) {
        super("Ignore-file at '" + missingIgnoreFile + "' not found.", throwable);
        this.missingIgnoreFile = missingIgnoreFile;
    }

    public File getMissingIgnoreFile() {
        return missingIgnoreFile;
    }
}
