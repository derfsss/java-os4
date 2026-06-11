/*
 * Event pump for the AmigaOS 4 toolkit (Phase 4 M4).
 *
 * A single daemon thread polls every open Intuition window's IDCMP port,
 * translates the messages to AWT events (posted to the EventQueue), and
 * blits dirty framebuffers into the windows (WritePixelArray).
 *
 * GPLv2+Classpath-exception (java-os4 project).
 */
package sun.awt.amiga;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.List;
import sun.awt.SunToolkit;

final class AmigaEventPump implements Runnable {

    /* Amiga input event codes / qualifiers (devices/inputevent.h) */
    private static final int RAW_LBUTTON = 0x68;
    private static final int RAW_RBUTTON = 0x69;
    private static final int RAW_MBUTTON = 0x6A;

    private static final int IEQ_LSHIFT  = 0x0001;
    private static final int IEQ_RSHIFT  = 0x0002;
    private static final int IEQ_CONTROL = 0x0008;
    private static final int IEQ_LALT    = 0x0010;
    private static final int IEQ_RALT    = 0x0020;
    private static final int IEQ_LAMIGA  = 0x0040;
    private static final int IEQ_RAMIGA  = 0x0080;
    private static final int IEQ_MIDBUTTON  = 0x1000;
    private static final int IEQ_RBUTTON    = 0x2000;
    private static final int IEQ_LEFTBUTTON = 0x4000;

    /** key-event tracing (-Damiga.debug.keys=true) */
    private static final boolean DEBUG_KEYS =
        Boolean.getBoolean("amiga.debug.keys");

    /** settle time before a dirty frame is blitted */
    private static final int BLIT_SETTLE_MS = 30;
    /** but never let a continuously-repainted frame starve longer than this */
    private static final int BLIT_FORCE_MS = 120;

    private static AmigaEventPump pump;

    private final List<AmigaWindowPeer> peers = new ArrayList<AmigaWindowPeer>();
    private final int[] ev = new int[8];

    /* click-count tracking */
    private long lastClickTime;
    private int lastClickX, lastClickY, lastClickButton, clickCount;

    static synchronized AmigaEventPump getPump() {
        if (pump == null) {
            pump = new AmigaEventPump();
            Thread t = new Thread(pump, "AWT-Amiga");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY + 1);
            t.start();
        }
        return pump;
    }

    void register(AmigaWindowPeer peer) {
        synchronized (peers) {
            if (!peers.contains(peer))
                peers.add(peer);
        }
    }

    void unregister(AmigaWindowPeer peer) {
        synchronized (peers) {
            peers.remove(peer);
        }
    }

    @Override
    public void run() {
        AmigaWindowPeer[] snapshot = new AmigaWindowPeer[0];
        while (true) {
            synchronized (peers) {
                snapshot = peers.toArray(snapshot.length == peers.size()
                                         ? snapshot : new AmigaWindowPeer[peers.size()]);
            }
            boolean sawEvent = false;
            for (AmigaWindowPeer p : snapshot) {
                if (p == null)
                    continue;
                long h = p.handle;
                if (h == 0)
                    continue;
                int type;
                int drained = 0;
                while ((type = AmigaNative.poll0(h, ev)) != AmigaNative.EV_NONE
                        && drained++ < 32) {
                    sawEvent = true;
                    translate(p, type);
                }
                maybeBlit(p);
            }
            try {
                Thread.sleep(sawEvent ? 5 : 15);
            } catch (InterruptedException ie) {
                return;
            }
        }
    }

    private void maybeBlit(AmigaWindowPeer p) {
        if (!p.dirty || p.handle == 0)
            return;
        long now = System.currentTimeMillis();
        if (now - p.lastDirtyAt < BLIT_SETTLE_MS
                && now - p.firstDirtyAt < BLIT_FORCE_MS)
            return;
        p.dirty = false;
        BufferedImage img = p.getImage();
        int[] px = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        AmigaNative.blit0(p.handle, px, 0, 0, img.getWidth(), img.getHeight(),
                          img.getWidth());
    }

    private void post(AmigaWindowPeer p, java.awt.AWTEvent e) {
        SunToolkit.postEvent(SunToolkit.targetToAppContext(p.target), e);
    }

    private static void dispatchOnEDT(final java.awt.AWTEvent e) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                ((java.awt.Component) e.getSource()).dispatchEvent(e);
            }
        });
    }

    private static void dispatchKeyOnEDT(final AmigaWindowPeer p, final int id,
                                         final long when, final int mods,
                                         final int keyCode, final char keyChar) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                java.awt.Component owner =
                    java.awt.KeyboardFocusManager
                        .getCurrentKeyboardFocusManager().getFocusOwner();
                java.awt.Component tgt = owner != null ? owner : p.target;
                tgt.dispatchEvent(new KeyEvent(tgt, id, when, mods,
                                               keyCode, keyChar));
            }
        });
    }

    private static int modifiers(int qual) {
        int m = 0;
        if ((qual & (IEQ_LSHIFT | IEQ_RSHIFT)) != 0)
            m |= InputEvent.SHIFT_DOWN_MASK;
        if ((qual & IEQ_CONTROL) != 0)
            m |= InputEvent.CTRL_DOWN_MASK;
        if ((qual & (IEQ_LALT | IEQ_RALT)) != 0)
            m |= InputEvent.ALT_DOWN_MASK;
        if ((qual & (IEQ_LAMIGA | IEQ_RAMIGA)) != 0)
            m |= InputEvent.META_DOWN_MASK;
        if ((qual & IEQ_LEFTBUTTON) != 0)
            m |= InputEvent.BUTTON1_DOWN_MASK;
        if ((qual & IEQ_MIDBUTTON) != 0)
            m |= InputEvent.BUTTON2_DOWN_MASK;
        if ((qual & IEQ_RBUTTON) != 0)
            m |= InputEvent.BUTTON3_DOWN_MASK;
        return m;
    }

    private void translate(AmigaWindowPeer p, int type) {
        int code = ev[0];
        int x = ev[1];
        int y = ev[2];
        int qual = ev[3];
        int ch = ev[4];
        long when = System.currentTimeMillis();

        /* a modal dialog blocks this window: swallow input (AWT's filter does
           the EDT-level block, this stops native input from even queuing) but
           still let close/refresh/activation through */
        if (p.isModalBlocked()) {
            switch (type) {
                case AmigaNative.EV_MOUSE_DOWN:
                case AmigaNative.EV_MOUSE_UP:
                case AmigaNative.EV_MOUSE_MOVE:
                case AmigaNative.EV_KEY_DOWN:
                case AmigaNative.EV_KEY_UP:
                    return;
                default:
                    break;
            }
        }

        switch (type) {
            case AmigaNative.EV_CLOSE:
                post(p, new WindowEvent(p.target, WindowEvent.WINDOW_CLOSING));
                break;

            case AmigaNative.EV_MOUSE_DOWN:
            case AmigaNative.EV_MOUSE_UP: {
                int button;
                int rc = code & 0x7F;
                if (rc == RAW_LBUTTON)
                    button = MouseEvent.BUTTON1;
                else if (rc == RAW_MBUTTON)
                    button = MouseEvent.BUTTON2;
                else if (rc == RAW_RBUTTON)
                    button = MouseEvent.BUTTON3;
                else
                    break;
                boolean press = (type == AmigaNative.EV_MOUSE_DOWN);
                int mods = modifiers(qual);
                int btnMask = button == MouseEvent.BUTTON1
                    ? InputEvent.BUTTON1_DOWN_MASK
                    : button == MouseEvent.BUTTON2
                        ? InputEvent.BUTTON2_DOWN_MASK
                        : InputEvent.BUTTON3_DOWN_MASK;
                if (press) {
                    mods |= btnMask;
                    if (when - lastClickTime < 400 && button == lastClickButton
                            && Math.abs(x - lastClickX) < 4
                            && Math.abs(y - lastClickY) < 4)
                        clickCount++;
                    else
                        clickCount = 1;
                    lastClickTime = when;
                    lastClickX = x;
                    lastClickY = y;
                    lastClickButton = button;
                } else {
                    mods &= ~btnMask;
                }
                boolean popup = press && button == MouseEvent.BUTTON3;
                post(p, new MouseEvent(p.target,
                    press ? MouseEvent.MOUSE_PRESSED : MouseEvent.MOUSE_RELEASED,
                    when, mods, x, y, clickCount, popup, button));
                if (!press)
                    post(p, new MouseEvent(p.target, MouseEvent.MOUSE_CLICKED,
                        when, mods, x, y, clickCount, false, button));
                break;
            }

            case AmigaNative.EV_MOUSE_MOVE: {
                int mods = modifiers(qual);
                boolean drag = (qual
                    & (IEQ_LEFTBUTTON | IEQ_MIDBUTTON | IEQ_RBUTTON)) != 0;
                /* synthesize window-level ENTER/EXIT from bounds crossing
                   (Intuition has no explicit pointer-left event); Swing's
                   LightweightDispatcher turns these + MOVED into per-component
                   rollover */
                boolean inside = x >= 0 && y >= 0
                    && x < p.target.getWidth() && y < p.target.getHeight();
                if (inside && !p.mouseInside) {
                    p.mouseInside = true;
                    post(p, new MouseEvent(p.target, MouseEvent.MOUSE_ENTERED,
                        when, mods, x, y, 0, false, MouseEvent.NOBUTTON));
                } else if (!inside && p.mouseInside) {
                    p.mouseInside = false;
                    post(p, new MouseEvent(p.target, MouseEvent.MOUSE_EXITED,
                        when, mods, x, y, 0, false, MouseEvent.NOBUTTON));
                }
                post(p, new MouseEvent(p.target,
                    drag ? MouseEvent.MOUSE_DRAGGED : MouseEvent.MOUSE_MOVED,
                    when, mods, x, y, 0, false, MouseEvent.NOBUTTON));
                break;
            }

            case AmigaNative.EV_KEY_DOWN:
            case AmigaNative.EV_KEY_UP: {
                boolean press = (type == AmigaNative.EV_KEY_DOWN);
                if (DEBUG_KEYS)
                    System.out.println("[KEY] " + (press ? "down" : "up")
                        + " raw=0x" + Integer.toHexString(code)
                        + " ch=" + ch + " qual=0x" + Integer.toHexString(qual));
                int raw = code & 0x7F;
                int keyCode = rawToVK(raw, ch);
                char keyChar = ch != 0 ? (char) ch : KeyEvent.CHAR_UNDEFINED;
                int mods = modifiers(qual);
                /* dispatch on the EDT, sourcing the event at the CURRENT focus
                   owner (resolved there -- the KFM holds it, not our peer) */
                dispatchKeyOnEDT(p, press ? KeyEvent.KEY_PRESSED
                                          : KeyEvent.KEY_RELEASED,
                                 when, mods, keyCode, keyChar);
                if (press && ch != 0 && isTypedChar(ch))
                    dispatchKeyOnEDT(p, KeyEvent.KEY_TYPED, when, mods,
                                     KeyEvent.VK_UNDEFINED, (char) ch);
                break;
            }

            case AmigaNative.EV_NEWSIZE:
                p.handleNativeResize(x, y);   /* vals[1]=innerW vals[2]=innerH */
                break;

            case AmigaNative.EV_MOVE:
                p.handleNativeMove(x, y);     /* inner-origin screen coords */
                break;

            case AmigaNative.EV_ACTIVATE:
                post(p, new sun.awt.TimedWindowEvent(p.target,
                    WindowEvent.WINDOW_GAINED_FOCUS, null, when));
                break;

            case AmigaNative.EV_DEACTIVATE:
                post(p, new sun.awt.TimedWindowEvent(p.target,
                    WindowEvent.WINDOW_LOST_FOCUS, null, when));
                break;

            case AmigaNative.EV_REFRESH:
                p.markDirty();
                break;

            default:
                break;
        }
    }

    private static boolean isTypedChar(int ch) {
        return (ch >= 32 && ch != 127) || ch == '\r' || ch == '\n'
            || ch == '\t' || ch == '\b';
    }

    /** Amiga RAWKEY -> Java VK code (US-keymap special keys; chars for the rest) */
    private static int rawToVK(int raw, int ch) {
        switch (raw) {
            case 0x40: return KeyEvent.VK_SPACE;
            case 0x41: return KeyEvent.VK_BACK_SPACE;
            case 0x42: return KeyEvent.VK_TAB;
            case 0x43: return KeyEvent.VK_ENTER;     /* numpad enter */
            case 0x44: return KeyEvent.VK_ENTER;
            case 0x45: return KeyEvent.VK_ESCAPE;
            case 0x46: return KeyEvent.VK_DELETE;
            case 0x4C: return KeyEvent.VK_UP;
            case 0x4D: return KeyEvent.VK_DOWN;
            case 0x4E: return KeyEvent.VK_RIGHT;
            case 0x4F: return KeyEvent.VK_LEFT;
            case 0x50: return KeyEvent.VK_F1;
            case 0x51: return KeyEvent.VK_F2;
            case 0x52: return KeyEvent.VK_F3;
            case 0x53: return KeyEvent.VK_F4;
            case 0x54: return KeyEvent.VK_F5;
            case 0x55: return KeyEvent.VK_F6;
            case 0x56: return KeyEvent.VK_F7;
            case 0x57: return KeyEvent.VK_F8;
            case 0x58: return KeyEvent.VK_F9;
            case 0x59: return KeyEvent.VK_F10;
            case 0x5F: return KeyEvent.VK_HELP;
            case 0x60:
            case 0x61: return KeyEvent.VK_SHIFT;
            case 0x62: return KeyEvent.VK_CAPS_LOCK;
            case 0x63: return KeyEvent.VK_CONTROL;
            case 0x64:
            case 0x65: return KeyEvent.VK_ALT;
            case 0x66:
            case 0x67: return KeyEvent.VK_META;
            default:
                if (ch >= 'a' && ch <= 'z')
                    return KeyEvent.VK_A + (ch - 'a');
                if (ch >= 'A' && ch <= 'Z')
                    return KeyEvent.VK_A + (ch - 'A');
                if (ch >= '0' && ch <= '9')
                    return KeyEvent.VK_0 + (ch - '0');
                return KeyEvent.VK_UNDEFINED;
        }
    }
}
