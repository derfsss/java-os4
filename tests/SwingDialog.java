/*
 * Phase 4 modal dialog test: JOptionPane (modal JDialog) opened from a
 * button action -- verifies the dialog peer, EDT modal pumping (the EDT
 * blocks in showMessageDialog while events keep flowing), multi-window
 * activation, and closing the dialog with a REAL click on its OK button.
 */
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class SwingDialog {

    static JFrame frame;
    static JButton button;
    static volatile boolean dialogClosed;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame = new JFrame("Dialog Test");
                button = new JButton("Open dialog");
                button.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
                button.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        System.out.println("[INFO] opening modal dialog");
                        JOptionPane.showMessageDialog(frame,
                            "Hello from a modal dialog!", "Modal",
                            JOptionPane.INFORMATION_MESSAGE);
                        dialogClosed = true;
                        System.out.println("[PASS] modal dialog returned");
                    }
                });
                frame.setContentPane(new javax.swing.JPanel(new BorderLayout()));
                frame.getContentPane().add(button, BorderLayout.CENTER);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(360, 200);
                frame.setVisible(true);
            }
        });
        System.out.println("[INFO] frame up");
        Thread.sleep(3000);

        /* open the modal dialog via the EDT (showMessageDialog blocks there;
           the secondary event pump must keep input flowing) */
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                button.doClick();
            }
        });

        for (int i = 0; i < 1200 && !dialogClosed; i++)  /* up to 10 min */
            Thread.sleep(500);

        System.out.println("[INFO] dialogClosed=" + dialogClosed);
        if (!dialogClosed)
            System.out.println("[FAIL] modal dialog never returned");

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.dispose();
            }
        });
        System.out.println("SwingDialog DONE");
        System.exit(0);
    }
}
