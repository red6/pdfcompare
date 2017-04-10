package de.redsix.pdfcompare;

import java.awt.image.BufferedImage;
import java.util.Objects;

public class ImageWithDimension {

    public final BufferedImage bufferedImage;
    public final float width;
    public final float height;

    public ImageWithDimension(final BufferedImage bufferedImage, final float width, final float height) {
        Objects.requireNonNull(bufferedImage, "bufferedImage was null");
        this.bufferedImage = bufferedImage;
        this.width = width;
        this.height = height;
    }
}
