/*
 * Phase 4 real modality test.  A custom modal JDialog whose OK button fills
 * the whole dialog (so injin can hit it without pixel-guessing).  Verifies:
 *   - the modal dialog blocks (the call after setVisible(true) only runs
 *     after the dialog hides);
 *   - clicks on the PARENT while modal are blocked (parentClicks stays 0);
 *   - a real click on the dialog button dismisses it and unblocks the parent.
 *
 * Geometry: parent frame at (80,60) 360x220; the modal dialog is created
 * 240x160 and (with our peer) also opens at (80,60), so its OK button --
 * filling the dialog -- is reliably around screen (200,140).
 */
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class SwingModal {

    static JFrame frame;
    static JButton openBtn;
    static JDialog dialog;
    static volatile boolean modalReturned;
    static volatile int parentClicks;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame = new JFrame("Modal Parent");
                openBtn = new JButton("(parent)");
                openBtn.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
                openBtn.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        parentClicks++;
                        System.out.println("[INFO] PARENT clicked (count="
                            + parentClicks + ")");
                    }
                });
                frame.setContentPane(new javax.swing.JPanel(new BorderLayout()));
                frame.getContentPane().add(openBtn, BorderLayout.CENTER);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setSize(360, 220);
                frame.setVisible(true);
            }
        });
        System.out.println("[INFO] frame up");
        Thread.sleep(3000);

        /* build + show the modal dialog on the EDT via invokeLater so the EDT
           blocks inside setVisible(true) -- the secondary pump must keep
           input flowing for the OK click to arrive */
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                dialog = new JDialog(frame, "Modal", true);  /* modal */
                JButton ok = new JButton("OK");
                ok.setFont(new Font(Font.DIALOG, Font.BOLD, 28));
                ok.addActionListener(new java.awt.event.ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        System.out.println("[INFO] dialog OK clicked");
                        dialog.dispose();
                    }
                });
                dialog.setContentPane(ok);          /* button fills the dialog */
                dialog.setSize(240, 160);
                System.out.println("[INFO] showing modal dialog (blocks here)");
                dialog.setVisible(true);            /* BLOCKS until dispose */
                modalReturned = true;
                System.out.println("[PASS] modal setVisible returned after OK");
            }
        });

        for (int i = 0; i < 1200 && !modalReturned; i++)
            Thread.sleep(500);

        System.out.println("[INFO] modalReturned=" + modalReturned
            + " parentClicks=" + parentClicks);
        if (modalReturned && parentClicks == 0)
            System.out.println("[PASS] modality enforced (parent blocked, "
                + "dialog dismissed by real click)");
        else
            System.out.println("[FAIL] modality: returned=" + modalReturned
                + " parentClicks=" + parentClicks);

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.dispose();
            }
        });
        System.out.println("SwingModal DONE");
        System.exit(0);
    }
}
