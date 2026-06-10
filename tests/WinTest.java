import java.awt.*;
import java.awt.image.*;

public class WinTest {
    public static void main(String[] a) throws Exception {
        int W = 320, H = 200;
        long win = AmigaWindow.open0(W, H, "Java on AmigaOS 4");
        if (win == 0) { System.out.println("[FAIL] open window"); return; }
        System.out.println("[PASS] window opened");

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setPaint(new GradientPaint(0, 0, new Color(0, 32, 96), 0, H, new Color(0, 160, 224)));
        g.fillRect(0, 0, W, H);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(255, 80, 0));
        g.fillOval(220, 110, 70, 70);
        g.setColor(Color.WHITE);
        g.fillRoundRect(18, 120, 120, 50, 16, 16);
        g.setColor(new Color(0, 32, 96));
        g.setFont(new Font("Dialog", Font.BOLD, 16));
        g.drawString("JamVM + OpenJDK 8", 24, 142);
        g.drawString("Java2D + freetype", 24, 160);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Dialog", Font.BOLD, 28));
        g.drawString("Hello, Amiga!", 20, 60);
        g.dispose();

        int[] px = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        AmigaWindow.blit0(win, px, 0, 0, W, H, W);
        System.out.println("[PASS] frame blitted");

        int[] ev = new int[4];
        long until = System.currentTimeMillis() + 15000;
        int events = 0;
        outer:
        while (System.currentTimeMillis() < until) {
            int t;
            while ((t = AmigaWindow.poll0(win, ev)) != 0) {
                events++;
                if (t == 1) { System.out.println("[INFO] close gadget"); break outer; }
                if (t == 8) AmigaWindow.blit0(win, px, 0, 0, W, H, W);
            }
            Thread.sleep(50);
        }
        System.out.println("[INFO] events seen=" + events);
        AmigaWindow.close0(win);
        System.out.println("WinTest DONE");
    }
}
