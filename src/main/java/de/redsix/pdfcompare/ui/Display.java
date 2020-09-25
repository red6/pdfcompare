package de.redsix.pdfcompare.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

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
import javax.swing.WindowConstants;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

import de.redsix.pdfcompare.CompareResultWithExpectedAndActual;
import de.redsix.pdfcompare.PdfComparator;
import de.redsix.pdfcompare.RenderingException;

public class Display {
    private ViewModel viewModel;
    private JFrame frame;
    private ImagePanel leftPanel;
    private ImagePanel resultPanel;
    private JToggleButton expectedButton;
    private double dpi = 300;

    public void init() {
        viewModel = new ViewModel(new CompareResultWithExpectedAndActual());

        frame = new JFrame();
        final String title = "PDF Compare";
        frame.setTitle(title);
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
                leftPanel.setImage(viewModel.getLeftImage());
                resultPanel.setImage(viewModel.getDiffImage());
            }
        });

        addToolBarButton(toolBar, "Page +", (event) -> {
            if (viewModel.increasePage()) {
                leftPanel.setImage(viewModel.getLeftImage());
                resultPanel.setImage(viewModel.getDiffImage());
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
            leftPanel.setImage(viewModel.getLeftImage());
        });
        toolBar.add(expectedButton);
        buttonGroup.add(expectedButton);

        final JToggleButton actualButton = new JToggleButton("Actual");
        actualButton.addActionListener((event) -> {
            viewModel.showActual();
            leftPanel.setImage(viewModel.getLeftImage());
        });
        toolBar.add(actualButton);
        buttonGroup.add(actualButton);

        toolBar.addSeparator();

        final MouseListener mouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                double f = 300 / getDPI();
                double zoom = leftPanel.getZoomFactor() * f;
                String xy = (int) (e.getX() / zoom) + ", " + (int) (e.getY() / zoom);

                Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
                c.setContents(new StringSelection(xy), null);
                
                frame.setTitle(title + " - " + xy); // feedback for user
            }
        };
        final JToggleButton xyMode = new JToggleButton("XY Mode");
        xyMode.addActionListener(event -> {
            if (xyMode.isSelected()) {
                leftPanel.addMouseListener(mouseListener);
                // Cross mouse pointer shows active XY mode and makes positioning easier.
                leftPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            } else {
                leftPanel.removeMouseListener(mouseListener);
                leftPanel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
            frame.setTitle(title);
        });
        toolBar.add(xyMode);

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
    
            viewModel = new ViewModel(compareResult);
            leftPanel.setImage(viewModel.getLeftImage());
            resultPanel.setImage(viewModel.getDiffImage());
    
            if (compareResult.isEqual()) {
                JOptionPane.showMessageDialog(frame, "The compared documents are identical.");
            }
    
            expectedButton.setSelected(true);
        } finally {
            frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private static void DisplayExceptionDialog(final JFrame frame, final IOException ex) {
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

    public double getDPI() {
        return dpi;
    }

    public void setDPI(double dpi) {
        this.dpi = dpi;
    }
}
