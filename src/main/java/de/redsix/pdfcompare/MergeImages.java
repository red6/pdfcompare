package de.redsix.pdfcompare;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class MergeImages {

    private final int fontSize = 50;

	public ImageWithDimension mergeOnLeft(ImageWithDimension left, ImageWithDimension right, String headerLeft,
			String headerRight) {
        ImageWithDimension newImageWithDimension = null;
        try {
            ImageIO.write(left.getBufferedImage(), "PNG", new File("left.png"));
            ImageIO.write(right.getBufferedImage(), "PNG", new File("right.png"));

            float totalWidth = left.getBufferedImage().getWidth() + right.getBufferedImage().getWidth();
            float maxHeight = Math.max(left.getBufferedImage().getHeight(), right.getBufferedImage().getHeight());

            BufferedImage combinedGraphics = new BufferedImage(Math.round(totalWidth), Math.round(maxHeight), BufferedImage.TYPE_INT_ARGB);
            Graphics g = combinedGraphics.getGraphics();

            if (headerLeft != null && !headerLeft.isEmpty()) {
                Graphics leftGraphics = left.getBufferedImage().getGraphics();
                leftGraphics.setColor(Color.black);
                leftGraphics.setFont(new Font(g.getFont().getFontName(), Font.BOLD, 50));
                leftGraphics.drawString(headerLeft, Math.round(left.getBufferedImage().getWidth() / 2), fontSize);
            }
            if (headerRight != null && !headerRight.isEmpty()) {
                Graphics rightGraphics = right.getBufferedImage().getGraphics();
                rightGraphics.setColor(Color.black);
                rightGraphics.setFont(new Font(g.getFont().getFontName(), Font.BOLD, 50));
                rightGraphics.drawString(headerRight, Math.round(right.getBufferedImage().getWidth() / 2), fontSize);
            }

            g.drawImage(left.getBufferedImage(), 0, 0, null);
            g.drawImage(right.getBufferedImage(), Math.round(left.getBufferedImage().getWidth()), 0, null);
            ImageIO.write(combinedGraphics, "PNG", new File("comb.png"));
            g.dispose();
            newImageWithDimension = new ImageWithDimension(combinedGraphics, combinedGraphics.getWidth(), combinedGraphics.getHeight());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (newImageWithDimension);
    }
}
