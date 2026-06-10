/*
 * DialogPeer for the AmigaOS 4 toolkit (Phase 4 M4).
 * Modality is not enforced natively (modal dialogs behave modeless).
 * GPLv2+Classpath-exception (java-os4 project).
 */
package sun.awt.amiga;

import java.awt.Dialog;
import java.awt.Window;
import java.awt.peer.DialogPeer;
import java.util.List;

final class AmigaDialogPeer extends AmigaWindowPeer implements DialogPeer {

    AmigaDialogPeer(Dialog target) {
        super(target);
    }

    @Override
    public void setTitle(String title) {
        if (handle != 0)
            AmigaNative.settitle0(handle, title != null ? title : "");
    }

    @Override
    public void setResizable(boolean resizeable) {
    }

    @Override
    public void blockWindows(List<Window> windows) {
    }
}
