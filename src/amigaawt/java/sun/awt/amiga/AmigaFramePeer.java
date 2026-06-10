/*
 * FramePeer for the AmigaOS 4 toolkit (Phase 4 M4).
 * GPLv2+Classpath-exception (java-os4 project).
 */
package sun.awt.amiga;

import java.awt.Frame;
import java.awt.MenuBar;
import java.awt.Rectangle;
import java.awt.peer.FramePeer;

final class AmigaFramePeer extends AmigaWindowPeer implements FramePeer {

    AmigaFramePeer(Frame target) {
        super(target);
    }

    @Override
    public void setTitle(String title) {
        if (handle != 0)
            AmigaNative.settitle0(handle, title != null ? title : "");
    }

    @Override
    public void setMenuBar(MenuBar mb) {
        /* AWT menus unsupported -- Swing JMenuBar is lightweight and works */
    }

    @Override
    public void setResizable(boolean resizeable) {
    }

    @Override
    public void setState(int state) {
    }

    @Override
    public int getState() {
        return Frame.NORMAL;
    }

    @Override
    public void setMaximizedBounds(Rectangle bounds) {
    }

    @Override
    public void setBoundsPrivate(int x, int y, int width, int height) {
        setBounds(x, y, width, height, SET_BOUNDS);
    }

    @Override
    public Rectangle getBoundsPrivate() {
        return target.getBounds();
    }

    @Override
    public void emulateActivation(boolean activate) {
    }
}
