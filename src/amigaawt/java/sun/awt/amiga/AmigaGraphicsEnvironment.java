/*
 * GraphicsEnvironment for the AmigaOS 4 toolkit (Phase 4 M4).
 * GPLv2+Classpath-exception (java-os4 project).
 */
package sun.awt.amiga;

import java.awt.GraphicsDevice;
import sun.java2d.SunGraphicsEnvironment;
import sun.java2d.SurfaceManagerFactory;

public final class AmigaGraphicsEnvironment extends SunGraphicsEnvironment {

    static {
        SurfaceManagerFactory.setInstance(new AmigaSurfaceManagerFactory());
    }

    @Override
    protected int getNumScreens() {
        return 1;
    }

    @Override
    protected GraphicsDevice makeScreenDevice(int screennum) {
        return new AmigaGraphicsDevice();
    }

    @Override
    public boolean isDisplayLocal() {
        return true;
    }
}
