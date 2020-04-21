package x.team.tool;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import de.redsix.pdfcompare.ImageWithDimension;

public class MergeImages {

	public ImageWithDimension mergeOnLeft(ImageWithDimension left, ImageWithDimension right) {
		BufferedImage newImage = null;
		ImageWithDimension nuovaImmag =null;
		try {
			//ImageIO.write(left.getBufferedImage(), "PNG", new File("output/left.png"));
			//ImageIO.write(right.getBufferedImage(), "PNG", new File("output/right.png"));
			
			float totalWidth = left.getBufferedImage().getWidth() + right.getBufferedImage().getWidth();
			float maxHeight = Math.max(left.getBufferedImage().getHeight(), right.getBufferedImage().getHeight());
			
			//System.out.println("totalWidth:" + totalWidth);
			//System.out.println("maxHeight:" + maxHeight);
			BufferedImage combined = new BufferedImage(Math.round(totalWidth), Math.round(maxHeight),BufferedImage.TYPE_INT_ARGB);

			Graphics g = combined.getGraphics();
			g.drawImage(left.getBufferedImage(), 0, 0, null);
			g.drawImage(right.getBufferedImage(), Math.round(left.getBufferedImage().getWidth()), 0, null);
			//ImageIO.write(combined, "PNG", new File("output/comb.png"));
			g.dispose();
			nuovaImmag = new ImageWithDimension(combined, combined.getWidth(), combined.getHeight());
			//ImageIO.write(nuovaImmag.getBufferedImage(), "PNG", new File("output/combfinal.png"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (nuovaImmag);
	}
}
