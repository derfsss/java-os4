/*
 * WindowPeer for the AmigaOS 4 toolkit (Phase 4 M4).
 *
 * The Java window is backed by an INT_ARGB BufferedImage; all painting
 * (Swing/Java2D) renders into it, and the AmigaEventPump blits dirty frames
 * into the Intuition window with WritePixelArray.  Insets are reported as 0:
 * the Intuition borders live OUTSIDE the Java coordinate space (GimmeZeroZero
 * window, inner size == Java size).
 *
 * GPLv2+Classpath-exception (java-os4 project).
 */
package sun.awt.amiga;

import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.PaintEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.VolatileImage;
import java.awt.peer.ComponentPeer;
import java.awt.peer.ContainerPeer;
import java.awt.peer.WindowPeer;
import sun.awt.CausedFocusEvent;
import sun.awt.KeyboardFocusManagerPeerImpl;
import sun.awt.RepaintArea;
import sun.awt.SunToolkit;
import sun.awt.TimedWindowEvent;
import sun.awt.image.ToolkitImage;
import sun.font.FontDesignMetrics;
import sun.java2d.pipe.Region;

class AmigaWindowPeer implements WindowPeer {

    final Window target;

    /** native Intuition window handle, 0 while hidden */
    volatile long handle;

    /** the framebuffer Swing paints into; pump blits it */
    private BufferedImage image;
    private final Object imageLock = new Object();

    /** pump blit control */
    volatile boolean dirty;
    volatile long lastDirtyAt;
    volatile long firstDirtyAt;

    private final RepaintArea paintArea = new RepaintArea();

    private int width, height;
    private int lastSentX = Integer.MIN_VALUE, lastSentY = Integer.MIN_VALUE;

    AmigaWindowPeer(Window target) {
        this.target = target;
        Rectangle b = target.getBounds();
        width = Math.max(b.width, 1);
        height = Math.max(b.height, 1);
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        AmigaToolkit.peerCreated(target, this);
    }

    String windowTitle() {
        if (target instanceof Frame)
            return ((Frame) target).getTitle();
        if (target instanceof Dialog)
            return ((Dialog) target).getTitle();
        return "Java";
    }

    boolean isTargetResizable() {
        if (target instanceof Frame)
            return ((Frame) target).isResizable();
        if (target instanceof Dialog)
            return ((Dialog) target).isResizable();
        return false;
    }

    /* user resized the Intuition window (IDCMP_NEWSIZE): adopt the new inner
       size, then sync the AWT target on the EDT.  setBounds() sees the size
       already matching and only no-ops the native move -- no feedback loop. */
    void handleNativeResize(final int w, final int h) {
        synchronized (imageLock) {
            if (w == width && h == height)
                return;
            width = Math.max(w, 1);
            height = Math.max(h, 1);
            BufferedImage ni =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = ni.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            image = ni;
        }
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    target.setSize(w, h);
                    /* Component.reshape skips ComponentEvents for top-level
                       windows ("sent from peer or native code", 5025858) --
                       that is us */
                    target.dispatchEvent(new java.awt.event.ComponentEvent(
                        target, java.awt.event.ComponentEvent.COMPONENT_RESIZED));
                    target.validate();
                } catch (Throwable t) {
                    System.out.println("[RSZ] resize sync failed: " + t);
                }
            }
        });
        postPaint(0, 0, w, h);
    }

    BufferedImage getImage() {
        synchronized (imageLock) {
            return image;
        }
    }

    void markDirty() {
        long now = System.currentTimeMillis();
        lastDirtyAt = now;
        if (!dirty) {
            firstDirtyAt = now;
            dirty = true;
        }
    }

    /* ----------------------------- visibility ---------------------------- */

    @Override
    public void setVisible(boolean v) {
        if (v) {
            if (handle == 0) {
                handle = AmigaNative.open0(width, height, windowTitle(),
                                           isTargetResizable());
                if (handle != 0) {
                    AmigaEventPump.getPump().register(this);
                    markDirty();
                    /* tell the focus subsystem this window is now focused */
                    SunToolkit.postEvent(SunToolkit.targetToAppContext(target),
                        new TimedWindowEvent(target,
                            WindowEvent.WINDOW_GAINED_FOCUS, null,
                            System.currentTimeMillis()));
                    postPaint(0, 0, width, height);
                }
            }
        } else {
            if (handle != 0) {
                long h = handle;
                handle = 0;
                AmigaEventPump.getPump().unregister(this);
                AmigaNative.close0(h);
            }
        }
    }

    void postPaint(int x, int y, int w, int h) {
        SunToolkit.postEvent(SunToolkit.targetToAppContext(target),
            new PaintEvent(target, PaintEvent.PAINT, new Rectangle(x, y, w, h)));
    }

    /* ------------------------------ geometry ----------------------------- */

    @Override
    public void setBounds(int x, int y, int w, int h, int op) {
        w = Math.max(w, 1);
        h = Math.max(h, 1);
        boolean resized = (w != width) || (h != height);
        width = w;
        height = h;
        if (resized) {
            synchronized (imageLock) {
                BufferedImage ni =
                    new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = ni.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();
                image = ni;
            }
            if (handle != 0)
                AmigaNative.resize0(handle, w, h);
            postPaint(0, 0, w, h);
        }
        /* Only move when the Java-side position actually changes.  The first
           setBounds after open merely adopts Java's (insets-normalized) idea
           of the location -- the native window was placed by open0, and Java
           never knew that position; moving here would yank it to (0,~32). */
        if (handle != 0 && (x != lastSentX || y != lastSentY)) {
            boolean first = (lastSentX == Integer.MIN_VALUE);
            lastSentX = x;
            lastSentY = y;
            if (!first)
                AmigaNative.move0(handle, x, y);
        }
    }

    @Override
    public Point getLocationOnScreen() {
        Point p = target.getLocation();
        return new Point(p.x, p.y);
    }

    @Override
    public Dimension getPreferredSize() {
        return target.getSize();
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(1, 1);
    }

    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    /* ------------------------------ painting ----------------------------- */

    @Override
    public Graphics getGraphics() {
        Graphics2D g;
        synchronized (imageLock) {
            g = image.createGraphics();
        }
        Color fg = target.getForeground();
        Color bg = target.getBackground();
        Font f = target.getFont();
        g.setColor(fg != null ? fg : Color.BLACK);
        g.setBackground(bg != null ? bg : Color.WHITE);
        g.setFont(f != null ? f : new Font(Font.DIALOG, Font.PLAIN, 12));
        markDirty();
        return g;
    }

    @Override
    public void paint(Graphics g) {
        target.paint(g);
    }

    @Override
    public void print(Graphics g) {
        target.print(g);
    }

    @Override
    public void handleEvent(AWTEvent e) {
        if (e instanceof PaintEvent) {
            paintArea.paint(target, false);
            markDirty();
        }
    }

    @Override
    public void coalescePaintEvent(PaintEvent e) {
        paintArea.add(e.getUpdateRect(), e.getID());
    }

    @Override
    public void layout() {
    }

    /* ------------------------------- focus ------------------------------- */

    @Override
    public boolean requestFocus(Component lightweightChild, boolean temporary,
                                boolean focusedWindowChangeAllowed, long time,
                                CausedFocusEvent.Cause cause) {
        return KeyboardFocusManagerPeerImpl.deliverFocus(lightweightChild,
            target, temporary, focusedWindowChangeAllowed, time, cause,
            AmigaKeyboardFocusManagerPeer.getInstance().getCurrentFocusOwner());
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    /* ------------------------------- images ------------------------------ */

    @Override
    public ColorModel getColorModel() {
        return ColorModel.getRGBdefault();
    }

    @Override
    public Image createImage(ImageProducer producer) {
        return new ToolkitImage(producer);
    }

    @Override
    public Image createImage(int w, int h) {
        return new BufferedImage(Math.max(w, 1), Math.max(h, 1),
                                 BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public VolatileImage createVolatileImage(int w, int h) {
        return getGraphicsConfiguration()
            .createCompatibleVolatileImage(Math.max(w, 1), Math.max(h, 1));
    }

    @Override
    public boolean prepareImage(Image img, int w, int h, ImageObserver o) {
        return Toolkit.getDefaultToolkit().prepareImage(img, w, h, o);
    }

    @Override
    public int checkImage(Image img, int w, int h, ImageObserver o) {
        return Toolkit.getDefaultToolkit().checkImage(img, w, h, o);
    }

    @Override
    public GraphicsConfiguration getGraphicsConfiguration() {
        return java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
            .getDefaultScreenDevice().getDefaultConfiguration();
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {
        return FontDesignMetrics.getMetrics(font);
    }

    /* ----------------------------- WindowPeer ---------------------------- */

    @Override
    public void toFront() {
        if (handle != 0)
            AmigaNative.tofront0(handle);
    }

    @Override
    public void toBack() {
        if (handle != 0)
            AmigaNative.toback0(handle);
    }

    @Override
    public void updateAlwaysOnTopState() {
    }

    @Override
    public void updateFocusableWindowState() {
    }

    @Override
    public void setModalBlocked(Dialog blocker, boolean blocked) {
    }

    @Override
    public void updateMinimumSize() {
    }

    @Override
    public void updateIconImages() {
    }

    @Override
    public void setOpacity(float opacity) {
    }

    @Override
    public void setOpaque(boolean isOpaque) {
    }

    @Override
    public void updateWindow() {
        markDirty();
    }

    @Override
    public void repositionSecurityWarning() {
    }

    /* --------------------------- ContainerPeer --------------------------- */

    public void beginValidate() {
    }

    public void endValidate() {
    }

    public void beginLayout() {
    }

    public void endLayout() {
    }

    /* ------------------------- remaining plumbing ------------------------ */

    @Override
    public boolean isObscured() {
        return false;
    }

    @Override
    public boolean canDetermineObscurity() {
        return false;
    }

    @Override
    public void setEnabled(boolean e) {
    }

    @Override
    public void setForeground(Color c) {
    }

    @Override
    public void setBackground(Color c) {
    }

    @Override
    public void setFont(Font f) {
    }

    @Override
    public void updateCursorImmediately() {
    }

    @Override
    public boolean handlesWheelScrolling() {
        return false;
    }

    @Override
    public void createBuffers(int numBuffers, BufferCapabilities caps)
            throws AWTException {
        throw new AWTException("page flipping not supported");
    }

    @Override
    public Image getBackBuffer() {
        return null;
    }

    @Override
    public void flip(int x1, int y1, int x2, int y2,
                     BufferCapabilities.FlipContents flipAction) {
    }

    @Override
    public void destroyBuffers() {
    }

    @Override
    public void reparent(ContainerPeer newContainer) {
    }

    @Override
    public boolean isReparentSupported() {
        return false;
    }

    @Override
    public void applyShape(Region shape) {
    }

    @Override
    public void setZOrder(ComponentPeer above) {
    }

    @Override
    public boolean updateGraphicsData(GraphicsConfiguration gc) {
        return false;
    }

    @Override
    public void dispose() {
        setVisible(false);
        AmigaToolkit.peerDisposed(target, this);
    }
}
