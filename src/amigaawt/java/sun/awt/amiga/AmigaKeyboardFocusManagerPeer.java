/*
 * KeyboardFocusManagerPeer for the AmigaOS 4 toolkit (Phase 4 M4).
 * Single-app-context bookkeeping of the focused window / focus owner.
 * GPLv2+Classpath-exception (java-os4 project).
 */
package sun.awt.amiga;

import java.awt.Component;
import java.awt.Window;
import sun.awt.KeyboardFocusManagerPeerImpl;

public final class AmigaKeyboardFocusManagerPeer
        extends KeyboardFocusManagerPeerImpl {

    private static final AmigaKeyboardFocusManagerPeer instance =
        new AmigaKeyboardFocusManagerPeer();

    private Window focusedWindow;
    private Component focusOwner;

    static AmigaKeyboardFocusManagerPeer getInstance() {
        return instance;
    }

    private AmigaKeyboardFocusManagerPeer() {
    }

    @Override
    public synchronized void setCurrentFocusedWindow(Window win) {
        focusedWindow = win;
    }

    @Override
    public synchronized Window getCurrentFocusedWindow() {
        return focusedWindow;
    }

    @Override
    public synchronized void setCurrentFocusOwner(Component comp) {
        focusOwner = comp;
    }

    @Override
    public synchronized Component getCurrentFocusOwner() {
        return focusOwner;
    }
}
