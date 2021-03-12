package x.team.tool;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import de.redsix.pdfcompare.ImageWithDimension;

public class MergeImages {

	public ImageWithDimension mergeOnLeft(ImageWithDimension left, ImageWithDimension right, String headerLeft,
			String headerRight) {
		BufferedImage newImage = null;
		ImageWithDimension nuovaImmag = null;
		try {
			ImageIO.write(left.getBufferedImage(), "PNG", new File("left.png"));
			 ImageIO.write(right.getBufferedImage(), "PNG", new File("right.png"));

			float totalWidth = left.getBufferedImage().getWidth() + right.getBufferedImage().getWidth();
			float maxHeight = Math.max(left.getBufferedImage().getHeight(), right.getBufferedImage().getHeight());

			// System.out.println("totalWidth:" + totalWidth);
			// System.out.println("maxHeight:" + maxHeight);
			BufferedImage combined = new BufferedImage(Math.round(totalWidth), Math.round(maxHeight),
					BufferedImage.TYPE_INT_ARGB);
			Graphics g = combined.getGraphics();

			if (headerLeft != null && !headerLeft.isEmpty()) {
				Graphics leftG = left.getBufferedImage().getGraphics();
				leftG.setColor(Color.black);
				int fontSize = 50;
				leftG.setFont(new Font(g.getFont().getFontName(), Font.BOLD, 50));
				leftG.drawString(headerLeft, Math.round(left.getBufferedImage().getWidth() / 2), fontSize);
			}
			if (headerRight != null && !headerRight.isEmpty()) {
				Graphics rightG = right.getBufferedImage().getGraphics();
				rightG.setColor(Color.black);
				int fontSize = 50;
				rightG.setFont(new Font(g.getFont().getFontName(), Font.BOLD, 50));
				rightG.drawString(headerRight, Math.round(right.getBufferedImage().getWidth() / 2), fontSize);

			}

			//ImageIO.write(left.getBufferedImage(), "PNG", new File("left.png"));
			g.drawImage(left.getBufferedImage(), 0, 0, null);
			g.drawImage(right.getBufferedImage(), Math.round(left.getBufferedImage().getWidth()), 0, null);
			ImageIO.write(combined, "PNG", new File("comb.png"));
			g.dispose();
			nuovaImmag = new ImageWithDimension(combined, combined.getWidth(), combined.getHeight());
			// ImageIO.write(nuovaImmag.getBufferedImage(), "PNG", new
			// File("output/combfinal.png"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (nuovaImmag);
	}
}
