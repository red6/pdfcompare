package de.redsix.pdfcompare.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.border.BevelBorder;

import de.redsix.pdfcompare.CompareResultWithExpectedAndActual;
import de.redsix.pdfcompare.Exclusions;
import de.redsix.pdfcompare.PageArea;
import de.redsix.pdfcompare.env.DefaultEnvironment;
import de.redsix.pdfcompare.env.Environment;


public class ExclusionsPanel extends JPanel {
    
    private Environment environment;
    private JPanel exclusionsList;
    private JLabel configFile;
    /** currently selected file or directory for exports */
    private File selectedFile;
    /** original differences between PDF files for reference */
    private CompareResultWithExpectedAndActual compareResult;
    private ExclusionItemPanel selectedItem;
    private Display display;
    
    
    public ExclusionsPanel(Display display) {
        // FIXME: unsure what the role of the environment is
        environment = DefaultEnvironment.create();
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
        
        addToolBarButton(toolBar, "new", (event) -> addItemAction() );
        addToolBarButton(toolBar, "use diff", (event) -> useDifferencesAction() );
        addToolBarButton(toolBar, "load", (event) -> loadAction() );
        addToolBarButton(toolBar, "save", (event) -> saveAction() );
        
        
        // support to drag config files into the panel
        new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            
            @Override
            public void drop(DropTargetDropEvent dtde) {
                DataFlavor flavor = DataFlavor.javaFileListFlavor;
                
                if (! dtde.isDataFlavorSupported(flavor) ) {
                    dtde.rejectDrop();
                    return;
                }
                
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
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
                if (! dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ) {
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
        addExclusion(new PageArea(1));
    }
    
    private void useDifferencesAction() {
        useCompareResults();
        display.redrawImages();
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
                Display.DisplayExceptionDialog(frame, ex);
            }
        }
    }
    
    public void setCompareResults(CompareResultWithExpectedAndActual compareResult) {
        this.compareResult = compareResult;
        useCompareResults();
    }
    
    private void useCompareResults() {
        Exclusions exclusions = new Exclusions(environment);
        for (PageArea pageArea : compareResult.getDifferences()) {
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
    
    /** find item panel matching page area exactly */
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
    
    /** find item panel at page coordinates */
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
    
    /** highlights the item using a border */
    public void setSelectedItem(ExclusionItemPanel item) {
        if (selectedItem !=  null) {
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
    
    /** set data to UI */
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
    
    /** get data from UI */
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
                itemPanels.add( (ExclusionItemPanel) component);
            }
        }
        
        return itemPanels;
    }
    
    private void openExclusionFile(File exclusionFile) {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            selectedFile = exclusionFile;
            
            Exclusions exclusions = new Exclusions(environment);
            
            if (exclusionFile != null) {
                exclusions.readExclusions(exclusionFile);
                configFile.setText(exclusionFile.getName());
            } else {
                configFile.setText("unknown");
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
            Exclusions exclusions = getExclusions();
            
            List<String> exclusionLines = new ArrayList<>();
            try (FileWriter writer = new FileWriter(exclusionFile)) {
                exclusions.forEach(e -> {
                    exclusionLines.add(e.asJson());
                });
                
                // FIXME: this is destructive. We should merge the file
                writer.append("exclusions: [\n");
                writer.append(exclusionLines.stream().collect(Collectors.joining(",\n")));
                writer.append("\n]");
            }
        } finally {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }
    
    private static void addToolBarButton(final JToolBar toolBar, final String label, final ActionListener actionListener) {
        final JButton button = new JButton(label);
        button.addActionListener(actionListener);
        toolBar.add(button);
    }
    
    /** helper method to get the containing JFrame */
    private static JFrame getFrame(Container component) {
        while ( component != null && ! (component instanceof JFrame) ) {
            component = component.getParent();
        }
        
        return (JFrame) component;
    }
    
}