// NtpClock -- a 24-hour digital alarm-clock display (HH:MM:SS) for Java-OS4.
//
// It TRIES to fetch the time from an NTP server over SNTP/UDP and, if the
// network is unavailable (Java-OS4 currently ships a STUB libnet, so a
// DatagramSocket throws UnsatisfiedLinkError), it falls back to the Amiga
// system clock.  The status line shows which source is live.  On AmigaOS the
// recommended way to get NTP-accurate time is to run the OS's own NTP client
// (Roadshow/AmiTCP) which keeps the system clock correct -- this app then shows
// it.  The SNTP code is real and will activate once sockets are wired up.
//
// Run on Java-OS4:  jamvm-openjdk -cp NtpClock.jar NtpClock
//
// Java 8, Swing lightweights only, mouse-driven.  The display animates via a
// javax.swing.Timer on the EDT; the NTP query runs on a short-lived daemon
// thread (a blocking socket call must never run on the EDT) and reports back.

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Calendar;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class NtpClock {

    // ---- shared model: read on the EDT, written by the sync thread ----
    private static volatile long offsetMillis = 0L;        // ntpEpochMillis - localMillis
    private static volatile String source = "system clock";
    private static volatile boolean syncing = false;

    private static final String DEFAULT_SERVER = "pool.ntp.org";

    private static JLabel status;
    private static JTextField serverField;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { buildUI(); }
        });
    }

    private static void buildUI() {
        JFrame f = new JFrame("NTP Digital Clock");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final SevenSegPanel display = new SevenSegPanel();
        f.add(display, BorderLayout.CENTER);

        serverField = new JTextField(DEFAULT_SERVER, 13);
        JButton syncBtn = new JButton("Sync now");
        status = new JLabel("Using system clock");
        status.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        right.add(new JLabel("NTP:"));
        right.add(serverField);
        right.add(syncBtn);

        JPanel bottom = new JPanel(new BorderLayout(6, 0));
        bottom.add(status, BorderLayout.WEST);
        bottom.add(right, BorderLayout.EAST);
        f.add(bottom, BorderLayout.SOUTH);

        syncBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startSync(serverField.getText().trim());
            }
        });

        // Repaint ONLY when the displayed second changes (~1 Hz).  A full
        // antialiased 7-segment redraw + whole-window blit is expensive on the
        // JamVM interpreter, so repainting 5x/sec would hog the CPU and the
        // blitter and make the pointer / other windows sluggish.  The timer
        // still wakes 4x/sec, but a non-changing tick is just an integer
        // compare -- no render, no blit.  (The colon blinks at 1 Hz off the
        // second parity, which is the classic alarm-clock look anyway.)
        final long[] lastSec = { Long.MIN_VALUE };
        Timer t = new Timer(250, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                long sec = (System.currentTimeMillis() + offsetMillis) / 1000L;
                if (sec != lastSec[0]) {
                    lastSec[0] = sec;
                    display.repaint();
                    refreshStatus();
                }
            }
        });
        t.start();

        f.setSize(520, 260);
        f.setLocationRelativeTo(null);
        f.setVisible(true);

        startSync(DEFAULT_SERVER);   // first attempt; falls back if no network
    }

    private static void refreshStatus() {
        String s = syncing
            ? "Syncing with " + serverField.getText().trim() + " ..."
            : "Time source: " + source;
        if (!s.equals(status.getText())) status.setText(s);
    }

    // ---- NTP sync on a daemon thread (never block the EDT on a socket) ----
    private static void startSync(final String server) {
        if (syncing || server == null || server.length() == 0) return;
        syncing = true;
        Thread th = new Thread(new Runnable() {
            public void run() {
                long off = 0L;
                String src;
                try {
                    off = queryNtpOffset(server);
                    src = "NTP (" + server + ")";
                } catch (Throwable ex) {
                    // UnsatisfiedLinkError (stub libnet), SocketException, timeout, ...
                    off = 0L;
                    src = "system clock  [NTP failed: " + brief(ex) + "]";
                }
                offsetMillis = off;
                source = src;
                syncing = false;
            }
        }, "ntp-sync");
        th.setDaemon(true);
        th.start();
    }

    private static String brief(Throwable ex) {
        String m = ex.getClass().getSimpleName();
        if (ex.getMessage() != null) m += ": " + ex.getMessage();
        if (m.length() > 64) m = m.substring(0, 64);
        return m;
    }

    // Minimal SNTP (RFC 4330) client. Returns ntpEpochMillis - localMillis.
    private static long queryNtpOffset(String server) throws Exception {
        DatagramSocket sock = null;
        try {
            sock = new DatagramSocket();
            sock.setSoTimeout(3000);
            byte[] buf = new byte[48];
            buf[0] = 0x1B;                          // LI=0, VN=3, Mode=3 (client)
            InetAddress addr = InetAddress.getByName(server);
            DatagramPacket req = new DatagramPacket(buf, buf.length, addr, 123);
            long t0 = System.currentTimeMillis();
            sock.send(req);
            DatagramPacket resp = new DatagramPacket(buf, buf.length);
            sock.receive(resp);
            long t3 = System.currentTimeMillis();

            long secs = readUInt32(buf, 40);        // transmit timestamp, seconds since 1900
            long frac = readUInt32(buf, 44);        // ...and fractional seconds
            long ntpMillis = (secs - 2208988800L) * 1000L + ((frac * 1000L) >>> 32);
            long localMid = (t0 + t3) / 2L;         // correct for ~half the round-trip
            return ntpMillis - localMid;
        } finally {
            if (sock != null) sock.close();
        }
    }

    private static long readUInt32(byte[] b, int i) {
        return ((long) (b[i] & 0xff) << 24) | ((long) (b[i + 1] & 0xff) << 16)
             | ((long) (b[i + 2] & 0xff) << 8) | (long) (b[i + 3] & 0xff);
    }

    // ---- 7-segment display panel ----
    static class SevenSegPanel extends JPanel {
        // segments a,b,c,d,e,f,g for digits 0-9
        private static final boolean[][] SEG = {
            { true,  true,  true,  true,  true,  true,  false }, // 0
            { false, true,  true,  false, false, false, false }, // 1
            { true,  true,  false, true,  true,  false, true  }, // 2
            { true,  true,  true,  true,  false, false, true  }, // 3
            { false, true,  true,  false, false, true,  true  }, // 4
            { true,  false, true,  true,  false, true,  true  }, // 5
            { true,  false, true,  true,  true,  true,  true  }, // 6
            { true,  true,  true,  false, false, false, false }, // 7
            { true,  true,  true,  true,  true,  true,  true  }, // 8
            { true,  true,  true,  true,  false, true,  true  }, // 9
        };
        private final Color bg = new Color(18, 16, 15);
        private final Color on = new Color(255, 60, 40);
        private final Color off = new Color(48, 22, 19);

        SevenSegPanel() { setBackground(bg); }

        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            int W = getWidth(), H = getHeight();
            g2.setColor(bg);
            g2.fillRect(0, 0, W, H);

            long now = System.currentTimeMillis() + offsetMillis;
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(now);
            int hh = c.get(Calendar.HOUR_OF_DAY);
            int mm = c.get(Calendar.MINUTE);
            int ss = c.get(Calendar.SECOND);
            boolean colon = (ss % 2 == 0);

            int[] digits = { hh / 10, hh % 10, mm / 10, mm % 10, ss / 10, ss % 10 };

            double dh = H * 0.55;
            double dw = dh * 0.55;
            double colonW = dw * 0.5;
            double gap = dw * 0.18;
            double totalW = 6 * dw + 2 * colonW + 7 * gap;
            double x = (W - totalW) / 2.0;
            double y = (H - dh) / 2.0;

            for (int i = 0; i < 6; i++) {
                drawDigit(g2, digits[i], x, y, dw, dh);
                x += dw + gap;
                if (i == 1 || i == 3) {            // colon after HH and after MM
                    drawColon(g2, x, y, colonW, dh, colon);
                    x += colonW + gap;
                }
            }
        }

        private void drawColon(Graphics2D g2, double x, double y, double w, double h, boolean lit) {
            double r = w * 0.5;
            double cx = x + (w - r) / 2.0;
            g2.setColor(lit ? on : off);
            g2.fillOval((int) cx, (int) (y + h * 0.30), (int) r, (int) r);
            g2.fillOval((int) cx, (int) (y + h * 0.62), (int) r, (int) r);
        }

        private void drawDigit(Graphics2D g2, int d, double x, double y, double w, double h) {
            boolean[] s = SEG[d];
            double t = w * 0.16;                   // segment thickness
            double pad = t * 0.6;
            double midY = y + h / 2.0;
            hSeg(g2, x + pad, y,            w - 2 * pad, t, s[0]); // a (top)
            hSeg(g2, x + pad, midY - t / 2, w - 2 * pad, t, s[6]); // g (middle)
            hSeg(g2, x + pad, y + h - t,    w - 2 * pad, t, s[3]); // d (bottom)
            vSeg(g2, x,         y + pad,            t, (h / 2) - 1.5 * pad, s[5]); // f (top-left)
            vSeg(g2, x + w - t, y + pad,            t, (h / 2) - 1.5 * pad, s[1]); // b (top-right)
            vSeg(g2, x,         midY + pad * 0.5,   t, (h / 2) - 1.5 * pad, s[4]); // e (bottom-left)
            vSeg(g2, x + w - t, midY + pad * 0.5,   t, (h / 2) - 1.5 * pad, s[2]); // c (bottom-right)
        }

        private void hSeg(Graphics2D g2, double x, double y, double len, double th, boolean lit) {
            g2.setColor(lit ? on : off);
            int[] px = { (int) x, (int) (x + th / 2), (int) (x + len - th / 2),
                         (int) (x + len), (int) (x + len - th / 2), (int) (x + th / 2) };
            int[] py = { (int) (y + th / 2), (int) y, (int) y,
                         (int) (y + th / 2), (int) (y + th), (int) (y + th) };
            g2.fillPolygon(px, py, 6);
        }

        private void vSeg(Graphics2D g2, double x, double y, double th, double len, boolean lit) {
            g2.setColor(lit ? on : off);
            int[] px = { (int) (x + th / 2), (int) (x + th), (int) (x + th),
                         (int) (x + th / 2), (int) x, (int) x };
            int[] py = { (int) y, (int) (y + th / 2), (int) (y + len - th / 2),
                         (int) (y + len), (int) (y + len - th / 2), (int) (y + th / 2) };
            g2.fillPolygon(px, py, 6);
        }
    }
}
