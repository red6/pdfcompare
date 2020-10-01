package de.redsix.pdfcompare.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoundedRangeModel;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

import de.redsix.pdfcompare.CompareResultWithExpectedAndActual;
import de.redsix.pdfcompare.PageArea;
import de.redsix.pdfcompare.PdfComparator;
import de.redsix.pdfcompare.RenderingException;


public class Display {
    private ViewModel viewModel;
    private JFrame frame;
    private ImagePanel leftPanel;
    private ImagePanel resultPanel;
    private ExclusionsPanel exclusionsPanel;
    private JToggleButton expectedButton;
    private boolean showExclusions = false;
    /** used for drawing a new exclusion area with the mouse */
    private PageArea dragArea;

    public void init() {
        viewModel = new ViewModel(new CompareResultWithExpectedAndActual());

        frame = new JFrame();
        final String title = "PDF Compare Studio";
        frame.setTitle(title);
        List<Image> list = new ArrayList<>();
        list.add(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/pdfcomparestudio16.png")));
        list.add(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/pdfcomparestudio40.png")));
        frame.setIconImages(list);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        final BorderLayout borderLayout = new BorderLayout();
        frame.setLayout(borderLayout);
        frame.setMinimumSize(new Dimension(400, 200));
        final Rectangle screenBounds = getDefaultScreenBounds();
        frame.setSize(Math.min(screenBounds.width, 1700), Math.min(screenBounds.height, 1000));
        frame.setLocation(screenBounds.x, screenBounds.y);
        //            frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);

        JToolBar toolBar = new JToolBar();
        toolBar.setRollover(true);
        toolBar.setFloatable(false);
        frame.add(toolBar, BorderLayout.PAGE_START);

        leftPanel = new ImagePanel(viewModel.getLeftImage());
        resultPanel = new ImagePanel(viewModel.getDiffImage());

        JScrollPane expectedScrollPane = new JScrollPane(leftPanel);
        expectedScrollPane.setMinimumSize(new Dimension(200, 200));
        JScrollPane actualScrollPane = new JScrollPane(resultPanel);
        actualScrollPane.setMinimumSize(new Dimension(200, 200));
        actualScrollPane.getViewport().addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(final ComponentEvent e) {
                resultPanel.setViewSize(e.getComponent().getSize());
                super.componentResized(e);
            }
        });

        expectedScrollPane.getVerticalScrollBar().setModel(actualScrollPane.getVerticalScrollBar().getModel());
        expectedScrollPane.getHorizontalScrollBar().setModel(actualScrollPane.getHorizontalScrollBar().getModel());
        expectedScrollPane.getViewport().addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(final ComponentEvent e) {
                leftPanel.setViewSize(e.getComponent().getSize());
                super.componentResized(e);
            }
        });

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, expectedScrollPane, actualScrollPane);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(0.5);
        splitPane.setOneTouchExpandable(true);
        frame.add(splitPane, BorderLayout.CENTER);
        
        exclusionsPanel = new ExclusionsPanel(this);
        // panel is hidden until toggled
        exclusionsPanel.setVisible(showExclusions);
        frame.add(exclusionsPanel, BorderLayout.EAST);

        expectedButton = new JToggleButton("Expected");

        addToolBarButton(toolBar, "Open...", (event) -> {
            JFileChooser fileChooser = new JFileChooser();
            try {
                if (fileChooser.showDialog(frame, "Open expected PDF") == JFileChooser.APPROVE_OPTION) {
                    final File expectedFile = fileChooser.getSelectedFile();
                    final JPasswordField passwordForExpectedFile = askForPassword(expectedFile);

                    if (fileChooser.showDialog(frame, "Open actual PDF") == JFileChooser.APPROVE_OPTION) {
                        final File actualFile = fileChooser.getSelectedFile();
                        final JPasswordField passwordForActualFile = askForPassword(actualFile);

                        openFiles(expectedFile, actualFile, String.valueOf(passwordForExpectedFile.getPassword()), String.valueOf(passwordForActualFile.getPassword()));
                    }
                }
            } catch (IOException ex) {
                DisplayExceptionDialog(frame, ex);
            }
        });

        toolBar.addSeparator();

        addToolBarButton(toolBar, "Page -", (event) -> {
            if (viewModel.decreasePage()) {
                leftPanel.setImage(applyExclusions(viewModel.getLeftImage()));
                resultPanel.setImage(applyExclusions(viewModel.getDiffImage()));
            }
        });

        addToolBarButton(toolBar, "Page +", (event) -> {
            if (viewModel.increasePage()) {
                leftPanel.setImage(applyExclusions(viewModel.getLeftImage()));
                resultPanel.setImage(applyExclusions(viewModel.getDiffImage()));
            }
        });

        toolBar.addSeparator();

        final JToggleButton pageZoomButton = new JToggleButton("Zoom Page");
        pageZoomButton.setSelected(true);
        pageZoomButton.addActionListener((event) -> {
            leftPanel.zoomPage();
            resultPanel.zoomPage();
        });

        addToolBarButton(toolBar, "Zoom -", (event) -> {
            pageZoomButton.setSelected(false);
            leftPanel.decreaseZoom();
            resultPanel.decreaseZoom();
        });

        addToolBarButton(toolBar, "Zoom +", (event) -> {
            pageZoomButton.setSelected(false);
            leftPanel.increaseZoom();
            resultPanel.increaseZoom();
        });

        toolBar.add(pageZoomButton);

        addToolBarButton(toolBar, "Zoom 100%", (event) -> {
            pageZoomButton.setSelected(false);
            leftPanel.zoom100();
            resultPanel.zoom100();
        });

        toolBar.addSeparator();

        addToolBarButton(toolBar, "Center Split", (event) -> {
            splitPane.setDividerLocation(0.5);
            splitPane.revalidate();
        });

        toolBar.addSeparator();

        final ButtonGroup buttonGroup = new ButtonGroup();
        expectedButton.setSelected(true);
        expectedButton.addActionListener((event) -> {
            viewModel.showExpected();
            leftPanel.setImage(applyExclusions(viewModel.getLeftImage()));
        });
        toolBar.add(expectedButton);
        buttonGroup.add(expectedButton);

        final JToggleButton actualButton = new JToggleButton("Actual");
        actualButton.addActionListener((event) -> {
            viewModel.showActual();
            leftPanel.setImage(applyExclusions(viewModel.getLeftImage()));
        });
        toolBar.add(actualButton);
        buttonGroup.add(actualButton);

        toolBar.addSeparator();
        
        final MouseAdapter mouseListener = new MouseAdapter() {
            private boolean dragging = false;
            private Point startPoint;
            
            @Override
            public void mouseClicked(MouseEvent e) {
                // pick selected area by clicking on it
                double zoom = leftPanel.getZoomFactor();
                ExclusionItemPanel item = exclusionsPanel.getItemAt(viewModel.getPageToShow() + 1
                        , (int) (e.getX() / zoom)
                        , (int) (e.getY() / zoom) );
                if (item != null) {
                    exclusionsPanel.setSelectedItem(item);
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
                dragging = true;
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragging) {
                    Point endPoint = e.getPoint();
                    
                    double d = Point2D.distance(startPoint.x, startPoint.y, endPoint.x, endPoint.y);
                    // assuming user didn't drag if less than 8 pixels
                    if (d > 7) {
                        int pageNumber = viewModel.getPageToShow();
                        PageArea pageArea = getPageArea(pageNumber, endPoint);
                        exclusionsPanel.addExclusion(pageArea);
                        ExclusionItemPanel item = exclusionsPanel.getItemForArea(pageArea);
                        exclusionsPanel.setSelectedItem(item);
                    }
                    
                    dragging = false;
                    startPoint = null;
                    dragArea = null;
                    
                    redrawImages();
                }
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging) {
                    int pageNumber = viewModel.getPageToShow();
                    Point endPoint = e.getPoint();
                    PageArea pageArea = getPageArea(pageNumber, endPoint);
                    
                    dragArea = pageArea;
                    
                    redrawImages();
                }
            }
            
            private PageArea getPageArea(int pageNumber, Point endPoint) {
                double zoom = leftPanel.getZoomFactor();
                
                int x1 = (int) Math.min(startPoint.x/zoom, endPoint.x/zoom);
                int y1 = (int) Math.min(startPoint.y/zoom, endPoint.y/zoom);
                int x2 = (int) Math.max(startPoint.x/zoom, endPoint.x/zoom);
                int y2 = (int) Math.max(startPoint.y/zoom, endPoint.y/zoom);
                
                return new PageArea(pageNumber + 1, x1, y1, x2, y2);
            }
            
        };
        
        // zoom using the mouse wheel
        final MouseWheelListener mouseWheelListener = new MouseWheelListener() {
            
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                BoundedRangeModel horizontalModel = actualScrollPane.getHorizontalScrollBar().getModel();
                BoundedRangeModel verticalModel = actualScrollPane.getVerticalScrollBar().getModel();
                double horizontalOffset = horizontalModel.getValue();
                double verticalOffset = verticalModel.getValue();
                double horizontalExtent = horizontalModel.getExtent();
                double verticalExtent = verticalModel.getExtent();
                double zoomBefore = leftPanel.getZoomFactor();
                
                if (e.getWheelRotation() > 0) {
                    leftPanel.decreaseZoom();
                    resultPanel.decreaseZoom();
                    
                    // keep visible area centered on zooming out
                    double zoomAfter = leftPanel.getZoomFactor();
                    int horizontalValue = (int) ( (horizontalOffset + horizontalExtent / 2) * zoomAfter / zoomBefore - horizontalExtent / 2);
                    int verticalValue = (int) ( (verticalOffset + verticalExtent / 2) * zoomAfter / zoomBefore - verticalExtent / 2);
                    horizontalModel.setValue(horizontalValue);
                    verticalModel.setValue(verticalValue);

                } else {
                    leftPanel.increaseZoom();
                    resultPanel.increaseZoom();
                    
                    // keep visible area centered on mouse cursor
                    Point mousePoint = e.getPoint();
                    double zoomAfter = leftPanel.getZoomFactor();
                    int horizontalValue = (int) ( mousePoint.x * zoomAfter / zoomBefore - horizontalExtent / 2);
                    int verticalValue = (int) ( mousePoint.y * zoomAfter / zoomBefore - verticalExtent / 2);
                    
                    // FIXME: this is a workaround because the zoom isn't applied to the scrollbars immediately and scroll maximum might be too small at this time.
                    SwingUtilities.invokeLater(() -> {
                        horizontalModel.setValue(horizontalValue);
                        verticalModel.setValue(verticalValue);
                    });
                }
            }
            
        };
        leftPanel.addMouseWheelListener(mouseWheelListener);
        resultPanel.addMouseWheelListener(mouseWheelListener);
        
        final JToggleButton exclusionMode = new JToggleButton("Exclusions");
        exclusionMode.addActionListener(event -> {
            if (exclusionMode.isSelected()) {
                leftPanel.addMouseListener(mouseListener);
                leftPanel.addMouseMotionListener(mouseListener);
                resultPanel.addMouseListener(mouseListener);
                resultPanel.addMouseMotionListener(mouseListener);
                // Cross mouse pointer shows active XY mode and makes positioning easier.
                leftPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                showExclusions = true;
            } else {
                leftPanel.removeMouseListener(mouseListener);
                leftPanel.removeMouseMotionListener(mouseListener);
                resultPanel.removeMouseListener(mouseListener);
                resultPanel.removeMouseMotionListener(mouseListener);
                leftPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                showExclusions = false;
            }
            exclusionsPanel.setVisible(showExclusions);
            redrawImages();
            frame.setTitle(title);
        });
        toolBar.add(exclusionMode);

        frame.setVisible(true);
    }

    public void openFiles(File expectedFile, File actualFile,
            String passwordForExpectedFile, String passwordForActualFile) throws RenderingException, IOException {
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            PdfComparator<CompareResultWithExpectedAndActual> c = new PdfComparator<>(expectedFile, actualFile, new CompareResultWithExpectedAndActual());
            if (passwordForExpectedFile != null && !passwordForExpectedFile.isEmpty()) {
                c = c.withExpectedPassword(passwordForExpectedFile);
            }
            if (passwordForActualFile != null && !passwordForActualFile.isEmpty()) {
                c = c.withActualPassword(passwordForActualFile);
            }
            final CompareResultWithExpectedAndActual compareResult = (CompareResultWithExpectedAndActual) c.compare();
    
            exclusionsPanel.setCompareResults(compareResult);
            viewModel = new ViewModel(compareResult);
            leftPanel.setImage(applyExclusions(viewModel.getLeftImage()));
            resultPanel.setImage(applyExclusions(viewModel.getDiffImage()));
    
            if (compareResult.isEqual()) {
                JOptionPane.showMessageDialog(frame, "The compared documents are identical.");
            }
    
            expectedButton.setSelected(true);
        } finally {
            frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }
    
    public static void DisplayExceptionDialog(final JFrame frame, final Exception ex) {
        final StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));
        JTextArea textArea = new JTextArea(
                "An unexpected error has occurred: " + ex.getMessage() + "\n\n" + stringWriter);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(900, 700));
        JOptionPane.showMessageDialog(frame, scrollPane);
    }

    private static void addToolBarButton(final JToolBar toolBar, final String label, final ActionListener actionListener) {
        final JButton button = new JButton(label);
        button.addActionListener(actionListener);
        toolBar.add(button);
    }

    private static Rectangle getDefaultScreenBounds() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
    }

    private static JPasswordField askForPassword(final File file) throws IOException {
        JPasswordField passwordForFile = new JPasswordField(10);
        if (isInvalidPassword(file, "")) {
            final JLabel label = new JLabel("Enter password: ");
            label.setLabelFor(passwordForFile);

            final JPanel textPane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
            textPane.add(label);
            textPane.add(passwordForFile);

            JOptionPane.showMessageDialog(
                    null,
                    textPane,
                    "PDF is encrypted",
                    JOptionPane.INFORMATION_MESSAGE);

            label.setText("Password was invalid. Enter password: ");
            while (isInvalidPassword(file, String.valueOf(passwordForFile.getPassword()))) {
                passwordForFile.setText("");
                JOptionPane.showMessageDialog(
                        null,
                        textPane,
                        "PDF is encrypted",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        return passwordForFile;
    }

    private static boolean isInvalidPassword(final File file, final String password) throws IOException {
        try {
            PDDocument.load(file, password).close();
        } catch (InvalidPasswordException e) {
            return true;
        }
        return false;
    }

    public void redrawImages() {
        leftPanel.setImage(applyExclusions(viewModel.getLeftImage()));
        resultPanel.setImage(applyExclusions(viewModel.getDiffImage()));
    }
    
    /** switches to the page and scrolls to show the area. */
    public void showPageArea(PageArea pageArea) {
        int pageNo = pageArea.getPage();
        viewModel.setPageToShow(pageNo - 1);
        redrawImages();
        
        if (pageArea.getX1() != -1) {
            double zoom = leftPanel.getZoomFactor();

            Rectangle rect = new Rectangle(
                      (int) ( pageArea.getX1() * zoom )
                    , (int) ( pageArea.getY1() * zoom )
                    , (int) ( ( pageArea.getX2() - pageArea.getX1() ) * zoom )
                    , (int) ( ( pageArea.getY2() - pageArea.getY1() ) * zoom )
                );
            
            leftPanel.scrollRectToVisible(rect);
            frame.repaint();
        }
    }
    
    static private Color SHADE_BORDER = new Color(0x708090);
    static private Color SHADE = new Color(0x70606000, true);
    static private Color SHADE_HIGHLIGHT = new Color(0xA0A0A0FF, true);
    
    /** paints shaded areas over defined exclusions */
    public BufferedImage applyExclusions(BufferedImage image) {
        if (! showExclusions) {
            return image;
        }
        
        if (image == null) {
            return null;
        }
        
        int pageNo = viewModel.getPageToShow();
        
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB); 
        Graphics g = bufferedImage.getGraphics();
        
        g.drawImage(image, 0, 0, null);
        
        exclusionsPanel.getExclusions().forEach(pa -> {
            if (pa.getPage() != -1 && pa.getPage() != pageNo + 1) {
                // filter by page.
                // NOTE: forPage doesn't work correctly
                return;
            }
            
            if (pa.getX1() == -1) {
                // full page
                g.setColor(exclusionsPanel.isSelected(pa) ? SHADE_HIGHLIGHT : SHADE);
                g.fillRect(0, 0, image.getWidth(), image.getHeight());
                g.setColor(SHADE_BORDER);
                g.drawRect(0, 0, image.getWidth(), image.getHeight());
            } else {
                g.setColor(exclusionsPanel.isSelected(pa) ? SHADE_HIGHLIGHT : SHADE);
                g.fillRect(pa.getX1(), pa.getY1(), pa.getX2() - pa.getX1(), pa.getY2() - pa.getY1());
                g.setColor(SHADE_BORDER);
                g.drawRect(pa.getX1(), pa.getY1(), pa.getX2() - pa.getX1(), pa.getY2() - pa.getY1());
                
                if (dragArea == null && exclusionsPanel.isSelected(pa)) {
                    // draw scan lines for the selected area
                    ((Graphics2D) g).setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[] { 10, 20 }, 5 ));
                    g.drawLine(0, pa.getY1(), image.getWidth(), pa.getY1());
                    g.drawLine(0, pa.getY2(), image.getWidth(), pa.getY2());
                    g.drawLine(pa.getX1(), 0, pa.getX1(), image.getHeight());
                    g.drawLine(pa.getX2(), 0, pa.getX2(), image.getHeight());
                    ((Graphics2D) g).setStroke(new BasicStroke());
                }
            }
            
        });
        
        // this is the overlay for mouse drawing a rectangle
        if (dragArea != null) {
            g.setColor(SHADE_HIGHLIGHT);
            g.fillRect(dragArea.getX1(), dragArea.getY1(), dragArea.getX2() - dragArea.getX1(), dragArea.getY2() - dragArea.getY1());
            g.setColor(SHADE_BORDER);
            g.drawRect(dragArea.getX1(), dragArea.getY1(), dragArea.getX2() - dragArea.getX1(), dragArea.getY2() - dragArea.getY1());
            
            // draw scan lines for the area
            ((Graphics2D) g).setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[] { 10, 20 }, 5 ));
            g.drawLine(0, dragArea.getY1(), image.getWidth(), dragArea.getY1());
            g.drawLine(0, dragArea.getY2(), image.getWidth(), dragArea.getY2());
            g.drawLine(dragArea.getX1(), 0, dragArea.getX1(), image.getHeight());
            g.drawLine(dragArea.getX2(), 0, dragArea.getX2(), image.getHeight());
            ((Graphics2D) g).setStroke(new BasicStroke());
        }
        
        return bufferedImage;
    }

    /**
     * @return current page number, 1 based
     */
    public int getPageNumber() {
        return viewModel.getPageToShow() + 1;
    }
}
