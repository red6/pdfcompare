package de.redsix.pdfcompare.ui;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.*;

class ImagePanel extends JPanel implements Scrollable {

    private static final double[] zoomStages = {0.125, 0.250, 0.500, 1, 2, 4, 8};
    private BufferedImage image;
    private double zoom = 1;
    private boolean zoomPage = true;
    private int oldWidth;
    private int oldHeight;
    private Dimension viewSize;

    public ImagePanel(final BufferedImage image) {
        this.image = image;
        oldWidth = getImageWidth();
        oldHeight = getImageHeight();
    }

    private int getImageHeight() {
        return image == null ? 1 : image.getHeight();
    }

    private int getImageWidth() {
        return image == null ? 1 : image.getWidth();
    }

    @Override
    public void paintComponent(final Graphics g) {
        if (image == null) {
            return;
        }
        g.setColor(Color.DARK_GRAY);
        g.clearRect(0, 0, oldWidth, oldHeight);
        if (zoomPage) {
            setZoomToPage();
        }
        g.drawImage(image, 0, 0, getZoomWidth(), getZoomHeight(), null);
    }

    private void setZoomToPage() {
        double zoomWidth = ((double) viewSize.width) / ((double) getImageWidth());
        double zoomHeight = ((double) viewSize.height) / ((double) getImageHeight());
        zoom = Math.min(zoomWidth, zoomHeight);
    }

    private void getWidthZoom() {
        zoom = ((double) getVisibleRect().width) / ((double) getImageWidth());
    }

    private int getZoomWidth() {
        return (int) Math.floor(getImageWidth() * zoom);
    }

    private int getZoomHeight() {
        return (int) Math.floor(getImageHeight() * zoom);
    }

    @Override
    public Dimension getPreferredSize() {
        if (zoomPage) {
            return getParentSize();
        }
        return new Dimension(getZoomWidth(), getZoomHeight());
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        if (zoomPage) {
            return getParentSize();
        }
        return new Dimension(getZoomWidth(), getZoomHeight());
    }

    private Dimension getParentSize() {
        return viewSize == null
                ? new Dimension(getImageWidth(), getImageHeight())
                : viewSize;
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
        zoomPage = false;
        if (zoom < zoomStages[zoomStages.length - 1]) {
            int i = 0;
            for (; i < zoomStages.length && zoomStages[i] <= zoom; i++) {
            }
            zoom = zoomStages[i];
            revalidate();
        }
    }

    public void decreaseZoom() {
        zoomPage = false;
        if (zoom > zoomStages[0]) {
            oldWidth = getZoomWidth();
            oldHeight = getZoomHeight();
            int i = 0;
            for (; i < zoomStages.length && zoomStages[i] < zoom; i++) {
            }
            zoom = zoomStages[i - 1];
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

    public void zoomPage() {
        oldWidth = getZoomWidth();
        oldHeight = getZoomHeight();
        zoomPage = true;
        revalidate();
        repaint();
    }

    public void zoom100() {
        zoomPage = false;
        oldWidth = getZoomWidth();
        oldHeight = getZoomHeight();
        zoom = 1;
        revalidate();
        repaint();
    }

    public void setViewSize(final Dimension viewSize) {
        this.viewSize = viewSize;
        revalidate();
    }
}
