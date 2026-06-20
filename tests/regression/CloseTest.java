/*
 * CloseTest -- regression test for the "closing the window freezes the machine"
 * bug (amigans.net forum, afxgroup, Java-OS4 0.5.0).
 *
 * A Swing JFrame with the default EXIT_ON_CLOSE responds to WINDOW_CLOSING by
 * calling System.exit(0) WITHOUT disposing the frame, so the toolkit's
 * close0()/CloseWindow() never ran and the Intuition window was still open when
 * the process exited -- which hung the machine.  The fix is a native atexit()
 * handler in libamigaawt that closes any window still open at exit.
 *
 * This test opens a real (visible) window, then -- without any dispose() --
 * dispatches WINDOW_CLOSING to drive the exact EXIT_ON_CLOSE -> System.exit
 * path.  PASS == the VM exits cleanly and the Shell returns (no freeze).  The
 * [PASS] marker is printed from the WINDOW_CLOSING handler, just before exit.
 *
 * Run:  jamvm-openjdk -cp testsuite.zip CloseTest
 *       (a clean exit with rc 0 and a returned prompt is the pass signal)
 *
 * GPLv2 (java-os4 project).
 */
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class CloseTest {

    static JFrame frame;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame = new JFrame("CloseTest");
                JLabel l = new JLabel("closing in 2s (EXIT_ON_CLOSE)",
                                      JLabel.CENTER);
                l.setOpaque(true);
                l.setBackground(Color.WHITE);
                frame.setContentPane(l);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent e) {
                        System.out.println("[PASS] WINDOW_CLOSING handled;"
                            + " VM exiting cleanly (window must close, no hang)");
                    }
                });
                frame.setSize(320, 160);
                frame.setVisible(true);
            }
        });
        System.out.println("[INFO] CloseTest window up; will request close");

        Thread.sleep(2500);   // let the window open + register with the pump

        System.out.println("[INFO] dispatching WINDOW_CLOSING (EXIT_ON_CLOSE)");
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                frame.dispatchEvent(
                    new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }
        });

        /* EXIT_ON_CLOSE calls System.exit on the EDT; this main thread just
           waits to be torn down.  If we are still here after 10s the close
           path wedged -- report it (the host's run timeout is the real guard). */
        Thread.sleep(10000);
        System.out.println("[FAIL] still alive 10s after WINDOW_CLOSING -"
            + " close/exit did not complete");
        System.exit(2);
    }
}
