package de.redsix.pdfcompare;

import java.awt.*;
import java.awt.event.ActionListener;

import javax.swing.*;

public class Display {

    public Display(final CompareResult compareResult) {
        final ViewModel viewModel = new ViewModel(compareResult);

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

        expectedScrollPane.getVerticalScrollBar().setModel(actualScrollPane.getVerticalScrollBar().getModel());
        expectedScrollPane.getHorizontalScrollBar().setModel(actualScrollPane.getHorizontalScrollBar().getModel());

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, expectedScrollPane, actualScrollPane);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerLocation(0.5);
        splitPane.setOneTouchExpandable(true);
        frame.add(splitPane, BorderLayout.CENTER);

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

        addToolBarButton(toolBar, "Zoom -", (event) -> {
            leftPanel.decreaseZoom();
            resultPanel.decreaseZoom();
        });

        addToolBarButton(toolBar, "Zoom +", (event) -> {
            leftPanel.increaseZoom();
            resultPanel.increaseZoom();
        });

        toolBar.addSeparator();

        addToolBarButton(toolBar, "Center Split", (event) -> {
            splitPane.setDividerLocation(0.5);
            splitPane.revalidate();
        });

        toolBar.addSeparator();

        ButtonGroup buttonGroup = new ButtonGroup();
        JToggleButton expectedButton = new JToggleButton("Expected");
        expectedButton.doClick();
        expectedButton.addActionListener((event) -> {
            viewModel.showExpected();
            leftPanel.setImage(viewModel.getLeftImage());
        });
        toolBar.add(expectedButton);
        buttonGroup.add(expectedButton);

        JToggleButton actualButton = new JToggleButton("Actual");
        actualButton.addActionListener((event) -> {
            viewModel.showActual();
            leftPanel.setImage(viewModel.getLeftImage());
        });
        toolBar.add(actualButton);
        buttonGroup.add(actualButton);

        frame.setVisible(true);
    }

    private static void addToolBarButton(final JToolBar toolBar, final String label, final ActionListener actionListener) {
        final JButton button = new JButton(label);
        button.addActionListener(actionListener);
        toolBar.add(button);
    }

    private static Rectangle getDefaultScreenBounds() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
    }
}
