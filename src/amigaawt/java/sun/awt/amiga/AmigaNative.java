/*
 * JNI binding for libamigaawt (Phase 4 M4 AWT toolkit).
 * GPLv2+Classpath-exception (java-os4 project).
 */
package sun.awt.amiga;

class AmigaNative {

    static {
        System.loadLibrary("amigaawt");
    }

    /* event type codes (must match libamigaawt.c) */
    static final int EV_NONE       = 0;
    static final int EV_CLOSE      = 1;
    static final int EV_MOUSE_DOWN = 2;
    static final int EV_MOUSE_UP   = 3;
    static final int EV_MOUSE_MOVE = 4;
    static final int EV_KEY_DOWN   = 5;
    static final int EV_KEY_UP     = 6;
    static final int EV_NEWSIZE    = 7;
    static final int EV_REFRESH    = 8;
    static final int EV_ACTIVATE   = 9;
    static final int EV_DEACTIVATE = 10;
    static final int EV_MOVE       = 11;

    static native long open0(int w, int h, String title,
                              boolean sizable);
    static native void blit0(long h, int[] px, int x, int y, int w, int hgt,
                             int stride);
    /* out[0]=rawcode out[1]=x out[2]=y out[3]=qualifier out[4]=char */
    static native int poll0(long h, int[] out);
    static native void close0(long h);
    /* (width << 16) | height of the Workbench screen, 0 on failure */
    static native int screensize0();
    static native void resize0(long h, int w, int hgt);
    /* inner-origin screen position: (x << 16) | y */
    static native int winpos0(long h);
    static native void move0(long h, int x, int y);
    static native void settitle0(long h, String title);
    static native void tofront0(long h);
    static native void toback0(long h);

    private AmigaNative() {
    }
}
