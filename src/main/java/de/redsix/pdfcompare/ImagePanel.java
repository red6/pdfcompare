package de.redsix.pdfcompare;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.*;

class ImagePanel extends JPanel implements Scrollable {

    private BufferedImage image;
    private double zoom = 1;
    private int oldWidth;
    private int oldHeight;

    public ImagePanel(final BufferedImage image) {
        this.image = image;
        oldWidth = image.getWidth();
        oldHeight = image.getHeight();
    }

    @Override
    public void paintComponent(final Graphics g) {
        g.setColor(Color.DARK_GRAY);
        g.clearRect(0, 0, oldWidth, oldHeight);
        g.drawImage(image, 0, 0, getZoomWidth(), getZoomHeight(), null);
    }

    private int getZoomHeight() {
        return (int) (image.getHeight() * zoom);
    }

    private int getZoomWidth() {
        return (int) (image.getWidth() * zoom);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(getZoomWidth(), getZoomHeight());
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(getZoomWidth(), getZoomHeight());
    }

    @Override
    public int getScrollableUnitIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
        return (int) (40 * zoom);
    }

    @Override
    public int getScrollableBlockIncrement(final Rectangle visibleRect, final int orientation, final int direction) {
        if (orientation == SwingConstants.VERTICAL) {
            return visibleRect.height - 40;
        } else {
            return visibleRect.width - 40;
        }
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public void increaseZoom() {
        if (zoom < 8) {
            zoom = zoom * 2;
            revalidate();
        }
    }

    public void decreaseZoom() {
        if (zoom > 0.13) {
            oldWidth = getZoomWidth();
            oldHeight = getZoomHeight();
            zoom = zoom / 2;
            revalidate();
        }
    }

    public void setImage(final BufferedImage image) {
        oldWidth = getZoomWidth();
        oldHeight = getZoomHeight();
        this.image = image;
        revalidate();
        repaint();
    }
}
