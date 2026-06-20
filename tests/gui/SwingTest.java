/*
 * Phase 4 M4 bring-up test: a real Swing UI in an Intuition window.
 *
 * Run: jamvm-openjdk -Dawt.toolkit=sun.awt.amiga.AmigaToolkit
 *      -Djava.awt.graphicsenv=sun.awt.amiga.AmigaGraphicsEnvironment
 *      -cp swingtest.zip SwingTest
 *
 * Verifies: JFrame realization through the Amiga peer, Swing painting
 * (label, button, titled border) through Java2D into the framebuffer,
 * the EDT, and the action pipeline (programmatic doClick).
 */
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class SwingTest {

    static JFrame frame;
    static JLabel label;
    static JButton button;
    static volatile boolean clicked;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame = new JFrame("Swing on AmigaOS 4");
                label = new JLabel("Hello from Swing!", SwingConstants.CENTER);
                label.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
                button = new JButton("Click me");
                button.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        clicked = true;
                        label.setText("Button clicked!");
                    }
                });
                JPanel panel = new JPanel(new BorderLayout(8, 8));
                panel.setBorder(BorderFactory.createTitledBorder("JamVM + OpenJDK 8"));
                panel.add(label, BorderLayout.CENTER);
                panel.add(button, BorderLayout.SOUTH);
                frame.setContentPane(panel);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(360, 220);
                frame.setVisible(true);
            }
        });
        System.out.println("[PASS] JFrame visible");

        /* let the first paint reach the window */
        Thread.sleep(8000);

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                button.doClick();
            }
        });
        Thread.sleep(1000);
        if (clicked && "Button clicked!".equals(label.getText()))
            System.out.println("[PASS] action pipeline (doClick -> label updated)");
        else
            System.out.println("[FAIL] action pipeline");

        /* hold so the repainted UI can be observed / screenshotted */
        Thread.sleep(90000);

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.dispose();
            }
        });
        System.out.println("SwingTest DONE");
        System.exit(0);
    }
}
