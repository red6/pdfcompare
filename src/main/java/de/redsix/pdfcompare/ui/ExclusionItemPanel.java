package de.redsix.pdfcompare.ui;

import de.redsix.pdfcompare.PageArea;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;


/**
 * single exclusion area definition
 */
public class ExclusionItemPanel extends JPanel {
    static public final String INPUT_CHANGED = "input_changed";
    static public final String GOT_FOCUS = "got_focus";
    static public final String REMOVE = "remove";

    static private final PositiveIntegerVerifier VERIFIER = new PositiveIntegerVerifier();
    static private final Font LABEL_FONT = Font.decode("SansSerif-plain-10");
    static private final Color INVALID_COLOR = new Color(0xd08080);

    private JTextField page;
    private JTextField x1;
    private JTextField y1;
    private JTextField x2;
    private JTextField y2;
    private List<ActionListener> listeners = new ArrayList<>();
    private boolean invalid = false;
    private Color defaultBackground;


    public ExclusionItemPanel(PageArea pageArea) {
        defaultBackground = getBackground();

        init();

        setData(pageArea);
    }

    private void init() {
        this.setLayout(null);
        this.setPreferredSize(new Dimension(200, 45));

        this.add(createLabel("page", 5, 2, 30, 20));
        page = createIntField(5, 20, 30, 20);
        this.add(page);

        this.add(createLabel("x1", 40, 2, 35, 20));
        x1 = createIntField(40, 20, 35, 20);
        this.add(x1);

        this.add(createLabel("y1", 80, 2, 35, 20));
        y1 = createIntField(80, 20, 35, 20);
        this.add(y1);

        this.add(createLabel("x2", 120, 2, 35, 20));
        x2 = createIntField(120, 20, 35, 20);
        this.add(x2);

        this.add(createLabel("y2", 160, 2, 35, 20));
        y2 = createIntField(160, 20, 35, 20);
        this.add(y2);

        JButton deleteButton = new JButton("x");
        deleteButton.setToolTipText("delete item");
        deleteButton.setBorder(null);
        deleteButton.setBorderPainted(false);
        deleteButton.setBounds(200, 5, 12, 12);
        this.add(deleteButton);

        deleteButton.addActionListener(event -> notifyListeners(REMOVE));
    }

    private void setData(PageArea pageArea) {
        page.setText(toText(pageArea.getPage()));
        x1.setText(toText(pageArea.getX1()));
        y1.setText(toText(pageArea.getY1()));
        x2.setText(toText(pageArea.getX2()));
        y2.setText(toText(pageArea.getY2()));
    }

    public PageArea getData() {
        int pageValue = parseInt(page);
        int x1Value = parseInt(x1);
        int y1Value = parseInt(y1);
        int x2Value = parseInt(x2);
        int y2Value = parseInt(y2);

        boolean noPage = pageValue == -1;
        boolean noCoords = x1Value == -1 || y1Value == -1 || x2Value == -1 || y2Value == -1;

        setInvalid(false);
        try {
            if (noPage && noCoords) {
                return null;
            } else if (noPage) {
                return new PageArea(x1Value, y1Value, x2Value, y2Value);
            } else if (noCoords) {
                return new PageArea(pageValue);
            } else {
                return new PageArea(pageValue, x1Value, y1Value, x2Value, y2Value);
            }
        } catch (IllegalArgumentException exception) {
            setInvalid(true);
        }
        return null;
    }

    private String toText(int value) {
        if (value < 0) {
            return "";
        }
        return "" + value;
    }

    private int parseInt(JTextField field) {
        String text = field.getText();
        if (text == null || text.trim().length() == 0) {
            return -1;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private JLabel createLabel(String text, int x, int y, int width, int height) {
        JLabel pageLabel = new JLabel(text, SwingConstants.CENTER);
        pageLabel.setFont(LABEL_FONT);
        pageLabel.setBounds(x, y, width, height);
        return pageLabel;
    }

    private JTextField createIntField(int x, int y, int width, int height) {
        JTextField field = new JTextField();
        field.setInputVerifier(VERIFIER);
        field.setBounds(x, y, width, height);

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                notifyListeners(GOT_FOCUS);
            }

            @Override
            public void focusLost(FocusEvent event) {
                // calling to check validity
                getData();

                if (!isInvalid()) {
                    // notify main panel about valid coordinates
                    notifyListeners(INPUT_CHANGED);
                }
            }
        });
        return field;
    }

    public void setEditable(boolean editable) {
        page.setEditable(editable);
        x1.setEditable(editable);
        y1.setEditable(editable);
        x2.setEditable(editable);
        y2.setEditable(editable);
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;

        if (invalid) {
            this.setBackground(INVALID_COLOR);
            this.setOpaque(true);
        } else {
            // NOTE: the see through background can be visible in borders
            this.setBackground(defaultBackground);
            this.setOpaque(false);
        }
        this.revalidate();
    }

    public boolean isInvalid() {
        return invalid;
    }

    private void notifyListeners(String action) {
        for (ActionListener listener : listeners) {
            listener.actionPerformed(new ActionEvent(this, 0, action));
        }
    }

    public void addActionListener(ActionListener listener) {
        this.listeners.add(listener);
    }

    public void removeActionListener(ActionListener listener) {
        this.listeners.remove(listener);
    }

}