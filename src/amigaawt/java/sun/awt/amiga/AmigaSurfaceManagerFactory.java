/*
 * SurfaceManagerFactory for the AmigaOS 4 toolkit (Phase 4 M4).
 * No accelerated surfaces -- VolatileImages ride on the software
 * (BufferedImage) backup surface that VolatileSurfaceManager provides.
 * GPLv2+Classpath-exception (java-os4 project).
 */
package sun.awt.amiga;

import sun.awt.image.SunVolatileImage;
import sun.awt.image.SurfaceManager;
import sun.awt.image.VolatileSurfaceManager;
import sun.java2d.SurfaceData;
import sun.java2d.SurfaceManagerFactory;

public final class AmigaSurfaceManagerFactory extends SurfaceManagerFactory {

    @Override
    public VolatileSurfaceManager createVolatileManager(SunVolatileImage image,
                                                        Object context) {
        return new AmigaVolatileSurfaceManager(image, context);
    }

    private static final class AmigaVolatileSurfaceManager
            extends VolatileSurfaceManager {

        AmigaVolatileSurfaceManager(SunVolatileImage img, Object context) {
            super(img, context);
        }

        @Override
        protected boolean isAccelerationEnabled() {
            return false;
        }

        @Override
        protected SurfaceData initAcceleratedSurface() {
            return null;
        }
    }
}
