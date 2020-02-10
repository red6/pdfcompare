package de.redsix.pdfcompare.env;

import java.awt.*;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A SimpleEnvironment can be used to change environment settings programmatically.
 * All parameters, that were not explicitly set through setter are delegated to a
 * fallback Environment, which defaults to the regular environment backed by the
 * application.conf file.
 *
 * <pre>
 * new PdfComparator("expected.pdf", "actual.pdf")
 *         .withEnvironment(new SimpleEnvironment()
 *                 .setExpectedColor(Color.blue))
 *         .compare();
 * </pre>
 *
 * @see de.redsix.pdfcompare.PdfComparator#withEnvironment(Environment)
 */
public class SimpleEnvironment implements Environment {

    private final Environment fallback;

    private Path tempDirectory;
    private Integer nrOfImagesToCache;
    private Integer mergeCacheSize;
    private Integer swapCacheSize;
    private Integer documentCacheSize;
    private Integer maxImageSize;
    private Integer overallTimeout;
    private Boolean parallelProcessing;
    private Double allowedDiffInPercent;
    private Color expectedColor;
    private Color actualColor;
    private Integer dpi;

    public SimpleEnvironment() {
        this(DefaultEnvironment.create());
    }

    public SimpleEnvironment(Environment fallback) {
        Objects.requireNonNull(fallback, "fallback is null");
        this.fallback = fallback;
    }

    @Override
    public Path getTempDirectory() {
        return tempDirectory != null ? tempDirectory : fallback.getTempDirectory();
    }

    public SimpleEnvironment setTempDirectory(Path tempDirectory) {
        this.tempDirectory = tempDirectory;
        return this;
    }

    @Override
    public int getNrOfImagesToCache() {
        return nrOfImagesToCache != null ? nrOfImagesToCache : fallback.getNrOfImagesToCache();
    }

    public SimpleEnvironment setNrOfImagesToCache(int nrOfImagesToCache) {
        this.nrOfImagesToCache = nrOfImagesToCache;
        return this;
    }

    @Override
    public int getMergeCacheSize() {
        return mergeCacheSize != null ? mergeCacheSize : fallback.getMergeCacheSize();
    }

    public SimpleEnvironment setMergeCacheSize(int mergeCacheSize) {
        this.mergeCacheSize = mergeCacheSize;
        return this;
    }

    @Override
    public int getSwapCacheSize() {
        return swapCacheSize != null ? swapCacheSize : fallback.getSwapCacheSize();
    }

    public SimpleEnvironment setSwapCacheSize(int swapCacheSize) {
        this.swapCacheSize = swapCacheSize;
        return this;
    }

    @Override
    public int getDocumentCacheSize() {
        return documentCacheSize != null ? documentCacheSize : fallback.getDocumentCacheSize();
    }

    public SimpleEnvironment setDocumentCacheSize(int documentCacheSize) {
        this.documentCacheSize = documentCacheSize;
        return this;
    }

    @Override
    public int getMaxImageSize() {
        return maxImageSize != null ? maxImageSize : fallback.getMaxImageSize();
    }

    public SimpleEnvironment setMaxImageSize(int maxImageSize) {
        this.maxImageSize = maxImageSize;
        return this;
    }

    @Override
    public int getOverallTimeout() {
        return overallTimeout != null ? overallTimeout : fallback.getOverallTimeout();
    }

    public SimpleEnvironment setOverallTimeout(int overallTimeout) {
        this.overallTimeout = overallTimeout;
        return this;
    }

    @Override
    public boolean useParallelProcessing() {
        return parallelProcessing != null ? parallelProcessing : fallback.useParallelProcessing();
    }

    public SimpleEnvironment setParallelProcessing(boolean parallelProcessing) {
        this.parallelProcessing = parallelProcessing;
        return this;
    }

    @Override
    public double getAllowedDiffInPercent() {
        return allowedDiffInPercent != null ? allowedDiffInPercent : fallback.getAllowedDiffInPercent();
    }

    public SimpleEnvironment setAllowedDiffInPercent(double allowedDiffInPercent) {
        this.allowedDiffInPercent = allowedDiffInPercent;
        return this;
    }

    @Override
    public Color getExpectedColor() {
        return expectedColor != null ? expectedColor : fallback.getExpectedColor();
    }

    public SimpleEnvironment setExpectedColor(Color expectedColor) {
        this.expectedColor = expectedColor;
        return this;
    }

    @Override
    public Color getActualColor() {
        return actualColor != null ? actualColor : fallback.getActualColor();
    }

    public SimpleEnvironment setActualColor(Color actualColor) {
        this.actualColor = actualColor;
        return this;
    }

    public int getDPI() {
        return dpi != null ? dpi : fallback.getDPI();
    }

    public SimpleEnvironment setDPI(int dpi) {
        this.dpi = dpi;
        return this;
    }
}
