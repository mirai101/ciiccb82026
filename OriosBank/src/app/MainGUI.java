package app;

import gui.LoginFrame;
import javax.swing.SwingUtilities;

public class MainGUI {
    public static void main(String[] args) {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(LoginFrame::new);
    }
}
