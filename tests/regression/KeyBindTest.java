/*
 * KeyBindTest -- regression test for the "keyboard does not work in games" bug
 * (amigans.net forum, afxgroup, Java-OS4 0.5.0).
 *
 * Reproduces the Swing-game idiom that 0.5.0 got wrong: a focusable JPanel that
 * binds keys with WHEN_IN_FOCUSED_WINDOW and calls requestFocusInWindow() at
 * startup -- with NO click-to-focus.  In 0.5.0 the synthesized KeyEvent was
 * dispatched to the bare java.awt.Frame when the focus owner was null, so the
 * window-scoped bindings never fired and the game saw no keys.  With the fix
 * (AmigaEventPump.keyTarget falls back to the window's JRootPane, a JComponent),
 * the WHEN_IN_FOCUSED_WINDOW action fires.
 *
 * Drive it (host-side, via injin on the guest):
 *     injin KEY 0x40 KEY 0x4C KEY 0x4E
 * (0x40 = SPACE, 0x4C = cursor up, 0x4E = cursor right -- Amiga RAWKEY codes)
 *
 * PASS when the bound action fires at least once.
 *
 * GPLv2 (java-os4 project).
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

public class KeyBindTest {

    static JFrame frame;
    static JLabel status;
    static volatile int fires;
    static volatile String lastKey = "-";

    static void bind(JPanel p, String ks, final String name) {
        p.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke(ks), name);
        p.getActionMap().put(name, new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                fires++;
                lastKey = name;
                System.out.println("[PASS] WHEN_IN_FOCUSED_WINDOW action fired: "
                    + name + " (count=" + fires + ")");
                if (status != null)
                    status.setText("got " + name + " x" + fires);
            }
        });
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame = new JFrame("KeyBindTest");
                JPanel panel = new JPanel(new BorderLayout());
                panel.setBackground(Color.DARK_GRAY);
                status = new JLabel("press SPACE / arrows (no click)",
                                    JLabel.CENTER);
                status.setForeground(Color.WHITE);
                panel.add(status, BorderLayout.CENTER);

                /* the game idiom: window-scoped key bindings, focusable panel */
                bind(panel, "SPACE", "space");
                bind(panel, "UP",    "up");
                bind(panel, "DOWN",  "down");
                bind(panel, "LEFT",  "left");
                bind(panel, "RIGHT", "right");

                frame.setContentPane(panel);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(360, 200);
                frame.setVisible(true);
                panel.setFocusable(true);
                panel.requestFocusInWindow();   // NO click -- this is the case
            }
        });
        System.out.println("[INFO] KeyBindTest window up; inject keys now"
            + " (0x40=space 0x4C/4D/4E/4F=arrows)");

        for (int i = 0; i < 120 && fires == 0; i++)   // up to 60s
            Thread.sleep(500);

        if (fires > 0)
            System.out.println("[PASS] keyboard input reached the game ("
                + fires + " action(s), last=" + lastKey + ")");
        else
            System.out.println("[FAIL] no WHEN_IN_FOCUSED_WINDOW action fired -"
                + " keyboard input did not reach the game");

        System.out.println("KeyBindTest RESULT: " + (fires > 0 ? "PASS" : "FAIL"));
        System.out.println("KeyBindTest DONE");
        System.exit(fires > 0 ? 0 : 1);
    }
}
