/*
 * Phase 4 real-input test: REAL clicks and keystrokes (injected guest-side
 * via injin/input.device) into a Swing UI.
 *
 * Layout is two huge targets so screen->inner coordinate slop doesn't
 * matter: a JTextField filling the top half, a JButton the bottom half.
 * Verifies: real mouse click -> Swing focus (caret in field), real
 * keystrokes (incl. shift qualifier) -> KEY_TYPED -> document, real
 * click -> button action.
 *
 * Drive with:  injin POS 265 143 CLICK WAIT 600 KEY 0x25 KEY 0x17
 *              injin POS 265 253 CLICK
 * (window at 80,60 outer; 0x25='h' 0x17='i'; append S for shift)
 */
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class SwingType {

    static JFrame frame;
    static JTextField field;
    static JButton button;
    static volatile boolean focused, buttonClicked;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame = new JFrame("Type Test");
                field = new JTextField();
                field.setFont(new Font(Font.DIALOG, Font.BOLD, 28));
                field.addFocusListener(new java.awt.event.FocusAdapter() {
                    public void focusGained(java.awt.event.FocusEvent e) {
                        focused = true;
                        System.out.println("[PASS] field focused by real click");
                    }
                });
                button = new JButton("Press me");
                button.setFont(new Font(Font.DIALOG, Font.BOLD, 24));
                button.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        buttonClicked = true;
                        System.out.println("[PASS] button fired by real click");
                    }
                });
                frame.setContentPane(new javax.swing.JPanel(new GridLayout(2, 1)));
                frame.getContentPane().add(field);
                frame.getContentPane().add(button);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(360, 220);
                frame.setVisible(true);
            }
        });
        System.out.println("[INFO] frame up - inject input now");

        boolean typed = false;
        for (int i = 0; i < 240; i++) {   /* up to 120s */
            Thread.sleep(500);
            String t = field.getText();
            if (!typed && t.length() >= 2) {
                typed = true;
                System.out.println("[PASS] real typing arrived: \"" + t + "\"");
            }
            if (typed && buttonClicked)
                break;
        }
        System.out.println("[INFO] final field text: \"" + field.getText()
            + "\" focused=" + focused + " buttonClicked=" + buttonClicked);
        if (!(typed && focused && buttonClicked))
            System.out.println("[FAIL] real-input incomplete");

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.dispose();
            }
        });
        System.out.println("SwingType DONE");
        System.exit(0);
    }
}
