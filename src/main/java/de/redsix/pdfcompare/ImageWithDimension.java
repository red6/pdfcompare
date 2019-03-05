package de.redsix.pdfcompare;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.image.BufferedImage;

public class ImageWithDimension {

	public final BufferedImage bufferedImage;
	public final float width;
	public final float height;

	public ImageWithDimension(final BufferedImage bufferedImage, final float width, final float height) {
		checkNotNull(bufferedImage, "bufferedImage was null");
		this.bufferedImage = bufferedImage;
		this.width = width;
		this.height = height;
	}
}
