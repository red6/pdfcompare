package de.redsix.pdfcompare.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;

import de.redsix.pdfcompare.CompareResultWithExpectedAndActual;
import de.redsix.pdfcompare.PdfComparator;
import lombok.val;

public class Display {

    private ViewModel viewModel;

    public void init() {
        viewModel = new ViewModel(new CompareResultWithExpectedAndActual());

        val frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        final BorderLayout borderLayout = new BorderLayout();
        frame.setLayout(borderLayout);
        frame.setMinimumSize(new Dimension(400, 200));
        final Rectangle screenBounds = getDefaultScreenBounds();
        frame.setSize(Math.min(screenBounds.width, 1700), Math.min(screenBounds.height, 1000));
        frame.setLocation(screenBounds.x, screenBounds.y);
        //            frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);

        val toolBar = new JToolBar();
        toolBar.setRollover(true);
        toolBar.setFloatable(false);
        frame.add(toolBar, BorderLayout.PAGE_START);

        val leftPanel = new ImagePanel(viewModel.getLeftImage());
        val resultPanel = new ImagePanel(viewModel.getDiffImage());

        val expectedScrollPane = new JScrollPane(leftPanel);
        expectedScrollPane.setMinimumSize(new Dimension(200, 200));
        val actualScrollPane = new JScrollPane(resultPanel);
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

        addToolBarButton(toolBar, "Open...", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
			    JFileChooser fileChooser = new JFileChooser();
			    if (fileChooser.showDialog(frame, "Open expected PDF") == JFileChooser.APPROVE_OPTION) {
			        val expectedFile = fileChooser.getSelectedFile();
			        if (fileChooser.showDialog(frame, "Open actual PDF") == JFileChooser.APPROVE_OPTION) {
			            val actualFile = fileChooser.getSelectedFile();
			            try {
			                frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			                val compareResult = (CompareResultWithExpectedAndActual)
			                        new PdfComparator<CompareResultWithExpectedAndActual>(expectedFile, actualFile, new CompareResultWithExpectedAndActual()).compare();
			                viewModel = new ViewModel(compareResult);
			                leftPanel.setImage(viewModel.getLeftImage());
			                resultPanel.setImage(viewModel.getDiffImage());
			                frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			                expectedButton.setSelected(true);
			            } catch (IOException ex) {
			                DisplayExceptionDialog(frame, ex);
			            }
			        }
			    }
			}
		});

        toolBar.addSeparator();

        addToolBarButton(toolBar, "Page -", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
			    if (viewModel.decreasePage()) {
			        leftPanel.setImage(viewModel.getLeftImage());
			        resultPanel.setImage(viewModel.getDiffImage());
			    }
			}
		});

        addToolBarButton(toolBar, "Page +", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
			    if (viewModel.increasePage()) {
			        leftPanel.setImage(viewModel.getLeftImage());
			        resultPanel.setImage(viewModel.getDiffImage());
			    }
			}
		});

        toolBar.addSeparator();

        final JToggleButton pageZoomButton = new JToggleButton("Zoom Page");
        pageZoomButton.setSelected(true);
        pageZoomButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
			    leftPanel.zoomPage();
			    resultPanel.zoomPage();
			}
		});

        addToolBarButton(toolBar, "Zoom -", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
			    pageZoomButton.setSelected(false);
			    leftPanel.decreaseZoom();
			    resultPanel.decreaseZoom();
			}
		});

        addToolBarButton(toolBar, "Zoom +", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
			    pageZoomButton.setSelected(false);
			    leftPanel.increaseZoom();
			    resultPanel.increaseZoom();
			}
		});

        toolBar.add(pageZoomButton);

        addToolBarButton(toolBar, "Zoom 100%", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
			    pageZoomButton.setSelected(false);
			    leftPanel.zoom100();
			    resultPanel.zoom100();
			}
		});

        toolBar.addSeparator();

        addToolBarButton(toolBar, "Center Split", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
			    splitPane.setDividerLocation(0.5);
			    splitPane.revalidate();
			}
		});

        toolBar.addSeparator();

        final ButtonGroup buttonGroup = new ButtonGroup();
        expectedButton.setSelected(true);
        expectedButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
			    viewModel.showExpected();
			    leftPanel.setImage(viewModel.getLeftImage());
			}
		});
        toolBar.add(expectedButton);
        buttonGroup.add(expectedButton);

        final JToggleButton actualButton = new JToggleButton("Actual");
        actualButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
			    viewModel.showActual();
			    leftPanel.setImage(viewModel.getLeftImage());
			}
		});
        toolBar.add(actualButton);
        buttonGroup.add(actualButton);

        frame.setVisible(true);
    }

    private static void DisplayExceptionDialog(final JFrame frame, final IOException ex) {
        final StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));
        JTextArea textArea = new JTextArea(
                "Es ist ein unerwarteter Fehler aufgetreten: " + ex.getMessage() + "\n\n" + stringWriter);
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
}
