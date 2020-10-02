package de.redsix.pdfcompare.ui;

import de.redsix.pdfcompare.CompareResultWithExpectedAndActual;
import de.redsix.pdfcompare.Exclusions;
import de.redsix.pdfcompare.PdfComparator;
import de.redsix.pdfcompare.cli.CliArguments;
import de.redsix.pdfcompare.env.DefaultEnvironment;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

public class Display {

    private final JFrame frame = new JFrame();
    private ViewModel viewModel = new ViewModel(new CompareResultWithExpectedAndActual());
    private final ImagePanel leftPanel = new ImagePanel(viewModel.getLeftImage());
    private final ImagePanel resultPanel = new ImagePanel(viewModel.getDiffImage());
    private final JToggleButton expectedButton = new JToggleButton("Expected");
    private final Exclusions exclusions = new Exclusions(DefaultEnvironment.create());

    public void init(CliArguments cliArguments) {
        init();
        if (cliArguments.hasFileArguments()) {
            try {
                cliArguments.getExclusionsFile().ifPresent(exclusions::readExclusions);
                openFiles(new File(cliArguments.getExpectedFile().get()), cliArguments.getExpectedPassword(),
                        new File(cliArguments.getActualFile().get()), cliArguments.getActualPassword(), cliArguments.getExclusionsFile());
            } catch (IOException ex) {
                displayExceptionDialog(frame, ex);
            }
        }
    }

    public void init() {
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

        addToolBarButton(toolBar, "Open...", event -> {
            JFileChooser fileChooser = new JFileChooser();
            try {
                if (fileChooser.showDialog(frame, "Open expected PDF") == JFileChooser.APPROVE_OPTION) {
                    final File expectedFile = fileChooser.getSelectedFile();
                    final Optional<String> passwordForExpectedFile = askForPasswordIfNeeded(expectedFile);

                    if (fileChooser.showDialog(frame, "Open actual PDF") == JFileChooser.APPROVE_OPTION) {
                        final File actualFile = fileChooser.getSelectedFile();
                        final Optional<String> passwordForActualFile = askForPasswordIfNeeded(actualFile);

                        openFiles(expectedFile, passwordForExpectedFile, actualFile, passwordForActualFile, Optional.empty());
                    }
                }
            } catch (IOException ex) {
                displayExceptionDialog(frame, ex);
            }
        });

        toolBar.addSeparator();

        addToolBarButton(toolBar, "Page -", event -> {
            if (viewModel.decreasePage()) {
                leftPanel.setImage(viewModel.getLeftImage());
                resultPanel.setImage(viewModel.getDiffImage());
            }
        });

        addToolBarButton(toolBar, "Page +", event -> {
            if (viewModel.increasePage()) {
                leftPanel.setImage(viewModel.getLeftImage());
                resultPanel.setImage(viewModel.getDiffImage());
            }
        });

        toolBar.addSeparator();

        final JToggleButton pageZoomButton = new JToggleButton("Zoom Page");
        pageZoomButton.setSelected(true);
        pageZoomButton.addActionListener(event -> {
            leftPanel.zoomPage();
            resultPanel.zoomPage();
        });

        addToolBarButton(toolBar, "Zoom -", event -> {
            pageZoomButton.setSelected(false);
            leftPanel.decreaseZoom();
            resultPanel.decreaseZoom();
        });

        addToolBarButton(toolBar, "Zoom +", event -> {
            pageZoomButton.setSelected(false);
            leftPanel.increaseZoom();
            resultPanel.increaseZoom();
        });

        toolBar.add(pageZoomButton);

        addToolBarButton(toolBar, "Zoom 100%", event -> {
            pageZoomButton.setSelected(false);
            leftPanel.zoom100();
            resultPanel.zoom100();
        });

        toolBar.addSeparator();

        addToolBarButton(toolBar, "Center Split", event -> {
            splitPane.setDividerLocation(0.5);
            splitPane.revalidate();
        });

        toolBar.addSeparator();

        final ButtonGroup buttonGroup = new ButtonGroup();
        expectedButton.setSelected(true);
        expectedButton.addActionListener(event -> {
            viewModel.showExpected();
            leftPanel.setImage(viewModel.getLeftImage());
        });
        toolBar.add(expectedButton);
        buttonGroup.add(expectedButton);

        final JToggleButton actualButton = new JToggleButton("Actual");
        actualButton.addActionListener(event -> {
            viewModel.showActual();
            leftPanel.setImage(viewModel.getLeftImage());
        });
        toolBar.add(actualButton);
        buttonGroup.add(actualButton);

        frame.setVisible(true);
    }

    private void openFiles(File expectedFile, Optional<String> passwordForExpectedFile, File actualFile, Optional<String> passwordForActualFile,
            Optional<String> exclusions)
            throws IOException {
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        PdfComparator<CompareResultWithExpectedAndActual> pdfComparator
                = new PdfComparator<>(expectedFile, actualFile, new CompareResultWithExpectedAndActual());
        passwordForExpectedFile.ifPresent(pdfComparator::withExpectedPassword);
        passwordForActualFile.ifPresent(pdfComparator::withActualPassword);
        exclusions.ifPresent(pdfComparator::withIgnore);
        final CompareResultWithExpectedAndActual compareResult = pdfComparator.compare();

        viewModel = new ViewModel(compareResult);
        leftPanel.setImage(viewModel.getLeftImage());
        resultPanel.setImage(viewModel.getDiffImage());

        if (compareResult.isEqual()) {
            JOptionPane.showMessageDialog(frame, "The compared documents are identical." + this.exclusions);
        }

        frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        expectedButton.setSelected(true);
    }

    private static void displayExceptionDialog(final JFrame frame, final IOException ex) {
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

    private static Optional<String> askForPasswordIfNeeded(final File file) throws IOException {
        if (!isInvalidPassword(file, "")) {
            return Optional.empty();
        }

        JPasswordField passwordField = new JPasswordField(20);
        final JLabel label = new JLabel("Enter password: ");
        label.setLabelFor(passwordField);

        final JPanel textPane = new JPanel(new FlowLayout(FlowLayout.TRAILING));
        textPane.add(label);
        textPane.add(passwordField);

        JOptionPane.showMessageDialog(
                null,
                textPane,
                "PDF is encrypted",
                JOptionPane.INFORMATION_MESSAGE);

        label.setText("Password was invalid. Enter password: ");
        while (isInvalidPassword(file, String.valueOf(passwordField.getPassword()))) {
            passwordField.setText("");
            JOptionPane.showMessageDialog(
                    null,
                    textPane,
                    "PDF is encrypted",
                    JOptionPane.ERROR_MESSAGE);
        }
        return Optional.of(String.valueOf(passwordField.getPassword()));
    }

    private static boolean isInvalidPassword(final File file, final String password) throws IOException {
        try {
            PDDocument.load(file, password).close();
        } catch (InvalidPasswordException e) {
            return true;
        }
        return false;
    }
}
