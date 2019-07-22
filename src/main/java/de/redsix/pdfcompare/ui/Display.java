package de.redsix.pdfcompare.ui;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.*;

import de.redsix.pdfcompare.CompareResultWithExpectedAndActual;
import de.redsix.pdfcompare.PdfComparator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

public class Display {

    private ViewModel viewModel;

    public void init() {
        viewModel = new ViewModel(new CompareResultWithExpectedAndActual());

        JFrame frame = new JFrame();
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

        ImagePanel leftPanel = new ImagePanel(viewModel.getLeftImage());
        ImagePanel resultPanel = new ImagePanel(viewModel.getDiffImage());

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

        final JToggleButton expectedButton = new JToggleButton("Expected");

        addToolBarButton(toolBar, "Open...", (event) -> {
            JFileChooser fileChooser = new JFileChooser();
            try {
                if (fileChooser.showDialog(frame, "Open expected PDF") == JFileChooser.APPROVE_OPTION) {
                    final File expectedFile = fileChooser.getSelectedFile();
                    final JPasswordField passwordForExpectedFile = askForPassword(expectedFile);

                    if (fileChooser.showDialog(frame, "Open actual PDF") == JFileChooser.APPROVE_OPTION) {
                        final File actualFile = fileChooser.getSelectedFile();
                        final JPasswordField passwordForActualFile = askForPassword(actualFile);

                        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        final CompareResultWithExpectedAndActual compareResult = (CompareResultWithExpectedAndActual)
                                new PdfComparator<>(expectedFile, actualFile,
                                        new CompareResultWithExpectedAndActual())
                                        .withExpectedPassword(String.valueOf(passwordForExpectedFile.getPassword()))
                                        .withActualPassword(String.valueOf(passwordForActualFile.getPassword()))
                                        .compare();
                        viewModel = new ViewModel(compareResult);
                        leftPanel.setImage(viewModel.getLeftImage());
                        resultPanel.setImage(viewModel.getDiffImage());
                        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        expectedButton.setSelected(true);
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

        frame.setVisible(true);
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
            PDDocument.load(file, password);
        } catch (InvalidPasswordException e) {
            return true;
        }
        return false;
    }
}
