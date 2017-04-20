package de.redsix.pdfcompare;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageTools {

    public static final int EXCLUDED_BACKGROUND_RGB = new Color(255, 255, 100).getRGB();

    public static BufferedImage blankImage(final BufferedImage image) {
        Graphics2D graphics = image.createGraphics();
        graphics.setPaint(Color.white);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        return image;
    }

    public static int fadeElement(final int i) {
        final Color color = new Color(i);
        return new Color(fade(color.getRed()), fade(color.getGreen()), fade(color.getBlue())).getRGB();
    }

    public static int fadeExclusion(final int i) {
        final Color color = new Color(i);
        if (color.getRed() > 245 && color.getGreen() > 245 && color.getBlue() > 245) {
            return EXCLUDED_BACKGROUND_RGB;
        }
        return fadeElement(i);
    }

    private static int fade(final int i) {
        return i + ((255 - i) * 3 / 5);
    }

    public static BufferedImage deepCopy(BufferedImage image) {
        return new BufferedImage(image.getColorModel(), image.copyData(null), image.getColorModel().isAlphaPremultiplied(), null);
    }
}
