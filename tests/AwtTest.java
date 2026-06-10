import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;

public class AwtTest {
    static int pass=0, fail=0;
    static void ok(String n){ System.out.println("[PASS] "+n); pass++; }
    static void bad(String n, Throwable t){ System.out.println("[FAIL] "+n+": "+t); fail++; }
    static void check(String n, boolean c){ if(c) ok(n); else bad(n,new RuntimeException("assert false")); }

    public static void main(String[] a) {
        try { check("headless", GraphicsEnvironment.isHeadless()); } catch(Throwable t){ bad("headless",t); }

        BufferedImage img = null;
        try { img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
              check("bufferedimage.create", img.getWidth()==100); } catch(Throwable t){ bad("bufferedimage.create",t); }

        Graphics2D g = null;
        try { g = img.createGraphics();
              check("creategraphics", g != null); } catch(Throwable t){ bad("creategraphics",t); }

        try { g.setColor(Color.RED); g.fillRect(10,10,50,50);
              check("fillrect", img.getRGB(20,20)==0xFFFF0000 && img.getRGB(5,5)==0xFF000000
                             && img.getRGB(59,59)==0xFFFF0000 && img.getRGB(60,60)==0xFF000000); } catch(Throwable t){ bad("fillrect",t); }

        try { g.setColor(Color.GREEN); g.drawLine(0,80,99,80);
              check("drawline", img.getRGB(50,80)==0xFF00FF00 && img.getRGB(50,81)!=0xFF00FF00); } catch(Throwable t){ bad("drawline",t); }

        try { g.setColor(Color.BLUE); g.fillOval(70,10,20,20);
              check("filloval", img.getRGB(80,20)==0xFF0000FF && img.getRGB(70,10)!=0xFF0000FF); } catch(Throwable t){ bad("filloval",t); }

        try { Path2D.Double p = new Path2D.Double();
              p.moveTo(10,90); p.lineTo(30,90); p.lineTo(20,70); p.closePath();
              g.setColor(Color.YELLOW); g.fill(p);
              check("fillpath", img.getRGB(20,88)==0xFFFFFF00); } catch(Throwable t){ bad("fillpath",t); }

        try { BufferedImage img2 = new BufferedImage(20,20,BufferedImage.TYPE_INT_RGB);
              Graphics2D g2 = img2.createGraphics(); g2.setColor(Color.CYAN); g2.fillRect(0,0,20,20); g2.dispose();
              g.drawImage(img2, 40, 65, null);
              check("blit.drawimage", img.getRGB(45,70)==0xFF00FFFF); } catch(Throwable t){ bad("blit.drawimage",t); }

        try { g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
              g.setColor(Color.WHITE); g.fill(new Ellipse2D.Double(0,0,8,8));
              int c = img.getRGB(4,4);
              check("antialias.fill", ((c>>16)&0xFF) > 200); } catch(Throwable t){ bad("antialias.fill",t); }

        try { AffineTransform saved = g.getTransform();
              g.rotate(Math.PI/4, 50, 50); g.setColor(Color.MAGENTA); g.fillRect(45,20,10,10);
              g.setTransform(saved);
              boolean found=false;
              for(int y=15;y<45 && !found;y++) for(int x=55;x<90 && !found;x++)
                  if(img.getRGB(x,y)==0xFFFF00FF) found=true;
              check("transform.fill", found); } catch(Throwable t){ bad("transform.fill",t); }

        try { int[] px = img.getRGB(0,0,100,100,null,0,100);
              long sum=0; for(int v : px) sum += (v & 0xFFFFFF) != 0 ? 1 : 0;
              check("getrgb.bulk", px.length==10000 && sum > 2500); } catch(Throwable t){ bad("getrgb.bulk",t); }

        System.out.println("SUMMARY: "+pass+" passed, "+fail+" failed");
    }
}
