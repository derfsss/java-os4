/*
 * GraphicsDevice for the AmigaOS 4 toolkit (Phase 4 M4) -- the Workbench
 * screen as a single raster screen device.
 * GPLv2+Classpath-exception (java-os4 project).
 */
package sun.awt.amiga;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;

public final class AmigaGraphicsDevice extends GraphicsDevice {

    private final AmigaGraphicsConfig config = new AmigaGraphicsConfig(this);

    @Override
    public int getType() {
        return TYPE_RASTER_SCREEN;
    }

    @Override
    public String getIDstring() {
        return "AmigaOS Workbench";
    }

    @Override
    public GraphicsConfiguration[] getConfigurations() {
        return new GraphicsConfiguration[] { config };
    }

    @Override
    public GraphicsConfiguration getDefaultConfiguration() {
        return config;
    }
}
