package de.redsix.pdfcompare.ui;

import javax.swing.*;


/** allow only valid numbers to be input into a JTextField.
 * Validity is verified by using Integer.parseInt on the text.
 */
public class PositiveIntegerVerifier extends InputVerifier {
    
    @Override
    public boolean verify(JComponent input) {
        if (input instanceof JTextField) {
            try {
                String text = ((JTextField) input).getText();
                if (text == null || text.length() == 0) {
                    return true;
                }
                int value = Integer.parseInt(text);
                if (value < 0) {
                    return false;
                }
                
            } catch (NumberFormatException exception) {
                return false;
            }
        }
        return true;
    }
}