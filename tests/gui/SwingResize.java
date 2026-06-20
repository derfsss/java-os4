/*
 * Phase 4 resize test: user-resize of the Intuition window (size gadget drag,
 * injected via injin) must flow IDCMP_NEWSIZE -> peer -> AWT setSize ->
 * Swing revalidate/repaint.
 *
 * Drive with (window outer 370x256 at 80,60; size gadget bottom-right):
 *   injin POS 444 311 WAIT 200 DOWN WAIT 200 MOVE 60 50 WAIT 200 MOVE 20 10
 *         WAIT 200 UP
 */
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class SwingResize {

    static JFrame frame;
    static JLabel label;
    static volatile boolean resized;
    static Dimension initial;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame = new JFrame("Resize Test");
                label = new JLabel("", SwingConstants.CENTER);
                label.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
                frame.setContentPane(new javax.swing.JPanel(new BorderLayout()));
                frame.getContentPane().add(label, BorderLayout.CENTER);
                frame.addComponentListener(new java.awt.event.ComponentAdapter() {
                    public void componentResized(java.awt.event.ComponentEvent e) {
                        Dimension d = frame.getSize();
                        label.setText(d.width + " x " + d.height);
                        if (initial != null && !d.equals(initial)) {
                            resized = true;
                            System.out.println("[PASS] user resize arrived: "
                                + d.width + "x" + d.height);
                        }
                    }
                });
                frame.setSize(360, 220);
                initial = frame.getSize();
                label.setText(initial.width + " x " + initial.height);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            }
        });
        System.out.println("[INFO] frame up - drag the size gadget now");

        for (int i = 0; i < 240 && !resized; i++)
            Thread.sleep(500);

        Thread.sleep(2000);   /* let the final repaint land for screenshots */
        System.out.println("[INFO] final size: " + frame.getSize().width + "x"
            + frame.getSize().height + " resized=" + resized);
        if (!resized)
            System.out.println("[FAIL] no resize observed");

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.dispose();
            }
        });
        System.out.println("SwingResize DONE");
        System.exit(0);
    }
}
