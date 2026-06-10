/*
 * GraphicsConfiguration for the AmigaOS 4 toolkit (Phase 4 M4).
 * The Workbench screen presented as a 32-bit direct-color raster.
 * GPLv2+Classpath-exception (java-os4 project).
 */
package sun.awt.amiga;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;

public final class AmigaGraphicsConfig extends GraphicsConfiguration {

    private final AmigaGraphicsDevice device;
    private static Rectangle screenBounds;

    AmigaGraphicsConfig(AmigaGraphicsDevice device) {
        this.device = device;
    }

    static synchronized Rectangle getScreenBounds() {
        if (screenBounds == null) {
            int wh = 0;
            try {
                wh = AmigaNative.screensize0();
            } catch (Throwable t) {
                /* fall through to the default */
            }
            if (wh != 0)
                screenBounds = new Rectangle(0, 0, (wh >> 16) & 0xFFFF,
                                             wh & 0xFFFF);
            else
                screenBounds = new Rectangle(0, 0, 1280, 800);
        }
        return screenBounds;
    }

    @Override
    public GraphicsDevice getDevice() {
        return device;
    }

    @Override
    public ColorModel getColorModel() {
        return new DirectColorModel(24, 0x00ff0000, 0x0000ff00, 0x000000ff);
    }

    @Override
    public ColorModel getColorModel(int transparency) {
        switch (transparency) {
            case Transparency.OPAQUE:
                return getColorModel();
            case Transparency.BITMASK:
            case Transparency.TRANSLUCENT:
                return ColorModel.getRGBdefault();
            default:
                return null;
        }
    }

    @Override
    public AffineTransform getDefaultTransform() {
        return new AffineTransform();
    }

    @Override
    public AffineTransform getNormalizingTransform() {
        return new AffineTransform();
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(getScreenBounds());
    }
}
