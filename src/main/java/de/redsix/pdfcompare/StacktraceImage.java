package de.redsix.pdfcompare;

import de.redsix.pdfcompare.env.Environment;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;

public class StacktraceImage {

    private final String msg;
    private final Throwable throwable;
    private final int width;
    private final int height;

    public StacktraceImage(String msg, Throwable t, Environment environment) {
        this.msg = msg;
        this.throwable = t;
        this.width = 8 * environment.getDPI();
        this.height = 11 * environment.getDPI();
    }

    public ImageWithDimension getBlankImage() {
        return new ImageWithDimension(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), width, height);
    }

    public ImageWithDimension getImage() {
        BufferedImage errorImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = errorImage.createGraphics();
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
        int nextY = drawString(graphics, msg, 100, 100);
        nextY += graphics.getFontMetrics().getHeight();
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        drawString(graphics, sw.toString(), 100, nextY);
        graphics.dispose();
        return new ImageWithDimension(errorImage, errorImage.getWidth(), errorImage.getHeight());
    }

    private int drawString(Graphics g, String text, int x, int y) {
        int lineHeight = g.getFontMetrics().getHeight();
        for (String line : text.split("\n")) {
            g.drawString(line, x, y += lineHeight);
        }
        return y;
    }
}
