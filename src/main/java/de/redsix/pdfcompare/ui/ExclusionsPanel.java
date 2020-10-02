package de.redsix.pdfcompare.ui;

import de.redsix.pdfcompare.Exclusions;
import de.redsix.pdfcompare.PageArea;
import de.redsix.pdfcompare.env.Environment;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class ExclusionsPanel extends JPanel {

    private Environment environment;
    private JPanel exclusionsList;
    private JLabel configFile;
    /**
     * currently selected file or directory for exports
     */
    private File selectedFile;
    private ExclusionItemPanel selectedItem;
    private Display display;
    private Collection<PageArea> differencesFromCompare = new ArrayList<>();

    public ExclusionsPanel(Display display, Environment environment) {
        this.environment = environment;
        this.display = display;
        init();
    }

    private void init() {
        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(220, 100));

        this.add(new JLabel("Excluded Areas"), BorderLayout.NORTH);

        exclusionsList = new JPanel();
        exclusionsList.setLayout(new BoxLayout(exclusionsList, BoxLayout.PAGE_AXIS));

        JPanel configPanel = new JPanel(null);
        configPanel.setPreferredSize(new Dimension(200, 40));
        JLabel configLabel = new JLabel("Config File:");
        configLabel.setFont(Font.decode("SansSerif-plain-10"));
        configLabel.setBounds(5, 0, 190, 15);
        configPanel.add(configLabel);

        configFile = new JLabel("unknown");
        configFile.setBounds(10, 15, 190, 20);
        configPanel.add(configFile);

        JPanel exclusionsWrapper = new JPanel(new BorderLayout());
        exclusionsWrapper.add(exclusionsList, BorderLayout.NORTH);
        exclusionsWrapper.add(configPanel, BorderLayout.SOUTH);

        JScrollPane scrollPane = new JScrollPane(exclusionsWrapper);
        this.add(scrollPane, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setRollover(true);
        toolBar.setFloatable(false);
        this.add(toolBar, BorderLayout.SOUTH);

        addToolBarButton(toolBar, "New", "Add new exclusion block", event -> addItemAction());
        addToolBarButton(toolBar, "Diffs", "Show computed differences", event -> showComputedDifferencesAction());
        addToolBarButton(toolBar, "Load", "Load exclusions file", event -> loadAction());
        addToolBarButton(toolBar, "Save", "Save exclusions file", event -> saveAction());

        // support to drag config files into the panel
        new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetAdapter() {

            @Override
            public void drop(DropTargetDropEvent dtde) {
                DataFlavor flavor = DataFlavor.javaFileListFlavor;

                if (!dtde.isDataFlavorSupported(flavor)) {
                    dtde.rejectDrop();
                    return;
                }

                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(flavor);
                    if (files == null || files.isEmpty()) {
                        dtde.dropComplete(false);
                        return;
                    }


                    File exclusionFile = files.get(0);
                    openExclusionFile(exclusionFile);

                    dtde.dropComplete(true);

                } catch (Exception e) {
                    dtde.dropComplete(false);
                }
            }

            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                if (!dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    dtde.rejectDrag();
                    return;
                }

                if (dtde.getDropAction() != DnDConstants.ACTION_COPY) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                }

            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                if (dtde.getDropAction() != DnDConstants.ACTION_COPY) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                }
            }

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {
                if (dtde.getDropAction() != DnDConstants.ACTION_COPY) {
                    dtde.acceptDrag(DnDConstants.ACTION_COPY);
                }
            }

        });
    }

    private void addItemAction() {
        addExclusion(new PageArea(display.getPageNumber()));
    }

    private void showComputedDifferencesAction() {
        if (exclusionsList.getComponents().length == 0 || askForReplacement()) {
            useDifferencesFromCompare();
            display.redrawImages();
        }
    }

    private boolean askForReplacement() {
        return JOptionPane.showConfirmDialog(null, "This replaces the current exclusions with the computed differences.", "Info",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private void loadAction() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(selectedFile);

        if (fileChooser.showDialog(this, "Open exclusions definition") == JFileChooser.APPROVE_OPTION) {
            openExclusionFile(fileChooser.getSelectedFile());
        }
    }

    private void saveAction() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(selectedFile);
        if (fileChooser.showDialog(this, "Save exclusions as") == JFileChooser.APPROVE_OPTION) {
            try {
                selectedFile = fileChooser.getSelectedFile();
                saveExclusionFile(selectedFile);
            } catch (IOException ex) {
                JFrame frame = getFrame(this);
                Display.displayExceptionDialog(frame, ex);
            }
        }
    }

    public void useDifferencesFromCompare(Collection<PageArea> differences) {
        differencesFromCompare = differences;
        useDifferencesFromCompare();
    }

    private void useDifferencesFromCompare() {
        Exclusions exclusions = new Exclusions(environment);
        for (PageArea pageArea : differencesFromCompare) {
            exclusions.add(pageArea);
        }
        createExclusionItems(exclusions);
    }

    public void addExclusion(PageArea pageArea) {
        Exclusions exclusions = getExclusions();
        exclusions.add(pageArea);
        createExclusionItems(exclusions);
    }

    private void removeItem(ExclusionItemPanel item) {
        exclusionsList.remove(item);
        exclusionsList.repaint();
        display.redrawImages();
    }

    /**
     * find item panel matching page area exactly
     */
    public ExclusionItemPanel getItemForArea(PageArea pageArea) {
        if (pageArea == null) {
            return null;
        }

        for (ExclusionItemPanel item : getExclusionItemPanels()) {
            PageArea itemArea = item.getData();
            if (pageArea.equals(itemArea)) {
                return item;
            }
        }
        return null;
    }

    /**
     * find item panel at page coordinates
     */
    public ExclusionItemPanel getItemAt(int pageNumber, int x, int y) {
        for (ExclusionItemPanel item : getExclusionItemPanels()) {
            PageArea itemArea = item.getData();
            if (itemArea != null
                    && (itemArea.getPage() == pageNumber || itemArea.getPage() == -1)
                    && itemArea.contains(x, y)) {
                return item;
            }
        }
        return null;
    }

    /**
     * highlights the item using a border
     */
    public void setSelectedItem(ExclusionItemPanel item) {
        if (selectedItem != null) {
            selectedItem.setBorder(null);
        }

        selectedItem = item;

        if (selectedItem != null) {
            selectedItem.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        }
        display.redrawImages();
    }

    public boolean isSelected(PageArea pageArea) {
        if (selectedItem == null) {
            return false;
        }

        PageArea selectedArea = selectedItem.getData();
        if (selectedArea == null) {
            return false;
        }
        return selectedArea.equals(pageArea);
    }

    /**
     * set data to UI
     */
    private void createExclusionItems(Exclusions exclusions) {
        exclusionsList.removeAll();

        exclusions.forEach(e -> {
            ExclusionItemPanel item = new ExclusionItemPanel(e);

            item.addActionListener(event -> {
                if (ExclusionItemPanel.INPUT_CHANGED.equals(event.getActionCommand())) {
                    display.redrawImages();
                }

                if (ExclusionItemPanel.GOT_FOCUS.equals(event.getActionCommand())) {
                    setSelectedItem((ExclusionItemPanel) event.getSource());
                }

                if (ExclusionItemPanel.REMOVE.equals(event.getActionCommand())) {
                    removeItem((ExclusionItemPanel) event.getSource());
                }
            });

            item.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    if (event.getClickCount() == 1) {
                        // single click -> set selected
                        if (isSelected(item.getData())) {
                            setSelectedItem(null);
                        } else {
                            setSelectedItem(item);
                        }

                    }
                    if (event.getClickCount() == 2) {
                        // double click -> show this item in viewer
                        PageArea pageArea = item.getData();
                        if (pageArea != null) {
                            display.showPageArea(pageArea);
                        }
                    }
                }
            });
            exclusionsList.add(item);
        });
        exclusionsList.revalidate();
    }

    /**
     * get data from UI
     */
    public Exclusions getExclusions() {
        Exclusions exclusions = new Exclusions(environment);

        for (Component component : exclusionsList.getComponents()) {
            if (component instanceof ExclusionItemPanel) {
                PageArea pageArea = ((ExclusionItemPanel) component).getData();
                // NOTE: invalid user input is ignored
                if (pageArea != null) {
                    exclusions.add(pageArea);
                }
            }
        }
        return exclusions;
    }

    public List<ExclusionItemPanel> getExclusionItemPanels() {
        List<ExclusionItemPanel> itemPanels = new ArrayList<>();
        for (Component component : exclusionsList.getComponents()) {
            if (component instanceof ExclusionItemPanel) {
                itemPanels.add((ExclusionItemPanel) component);
            }
        }
        return itemPanels;
    }

    void openExclusionFile(File exclusionFile) {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            selectedFile = exclusionFile;
            Exclusions exclusions = new Exclusions(environment);

            setSelectedFile(exclusionFile);

            if (exclusionFile != null) {
                exclusions.readExclusions(exclusionFile);
            }
            createExclusionItems(exclusions);
            display.redrawImages();
        } finally {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private void saveExclusionFile(File exclusionFile) throws IOException {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(exclusionFile.toPath())) {
                writer.write(getExclusions().asJson());
            }
        } finally {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private static void addToolBarButton(final JToolBar toolBar, final String label, final String tooltip, final ActionListener actionListener) {
        final JButton button = new JButton(label);
        button.addActionListener(actionListener);
        button.setToolTipText(tooltip);
        toolBar.add(button);
    }

    /**
     * helper method to get the containing JFrame
     */
    private static JFrame getFrame(Container component) {
        while (component != null && !(component instanceof JFrame)) {
            component = component.getParent();
        }
        return (JFrame) component;
    }

    public void setSelectedFile(File ef) {
        selectedFile = ef;
        if (ef != null) {
            configFile.setText(ef.getName());
        } else {
            configFile.setText("unknown");
        }
    }
}