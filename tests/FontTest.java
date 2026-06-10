import java.awt.*;
import java.awt.image.*;

public class FontTest {
    public static void main(String[] a) {
        int pass=0, fail=0;
        BufferedImage img = new BufferedImage(200, 60, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            Font f = new Font("Dialog", Font.PLAIN, 24);
            g.setFont(f);
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth("Hello");
            System.out.println("[INFO] font=" + f.getFontName() + " ascent=" + fm.getAscent() + " width(Hello)=" + w);
            if (fm.getAscent() > 5 && w > 20) { System.out.println("[PASS] metrics"); pass++; }
            else { System.out.println("[FAIL] metrics"); fail++; }
        } catch (Throwable t) { System.out.println("[FAIL] metrics: " + t); fail++; }
        try {
            g.setColor(Color.WHITE);
            g.drawString("Hello Amiga", 10, 40);
            int lit = 0;
            for (int y = 0; y < 60; y++)
                for (int x = 0; x < 200; x++)
                    if ((img.getRGB(x, y) & 0xFFFFFF) != 0) lit++;
            System.out.println("[INFO] lit pixels=" + lit);
            if (lit > 100) { System.out.println("[PASS] drawstring"); pass++; }
            else { System.out.println("[FAIL] drawstring (lit=" + lit + ")"); fail++; }
        } catch (Throwable t) { System.out.println("[FAIL] drawstring: " + t); fail++; }
        System.out.println("SUMMARY: " + pass + " passed, " + fail + " failed");
    }
}
