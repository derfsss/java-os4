/* Thin JNI binding for libamigaawt (Phase 4 M3 bring-up test). */
import java.lang.reflect.Field;
import java.util.Vector;

public class AmigaWindow {
    static {
        System.loadLibrary("amigaawt");
        ClassLoader cl = AmigaWindow.class.getClassLoader();
        System.out.println("[DIAG] AmigaWindow loader=" + cl);
        try {
            Field sf = ClassLoader.class.getDeclaredField("systemNativeLibraries");
            sf.setAccessible(true);
            Vector<?> sv = (Vector<?>) sf.get(null);
            System.out.println("[DIAG] systemNativeLibraries size=" + sv.size());
            for (Object o : sv) {
                Field nf = o.getClass().getDeclaredField("name"); nf.setAccessible(true);
                Field hf = o.getClass().getDeclaredField("handle"); hf.setAccessible(true);
                System.out.println("[DIAG] syslib=" + nf.get(o) + " handle=" + hf.get(o));
            }
        } catch (Throwable t) { System.out.println("[DIAG] sys: " + t); }
        try {
            if (cl != null) {
                Field f = ClassLoader.class.getDeclaredField("nativeLibraries");
                f.setAccessible(true);
                System.out.println("[DIAG] loader nativeLibraries size=" + ((Vector<?>) f.get(cl)).size());
            }
        } catch (Throwable t) { System.out.println("[DIAG] app: " + t); }
    }
    public static native long open0(int w, int h, String title);
    public static native void blit0(long h, int[] px, int x, int y, int w, int hgt, int stride);
    public static native int poll0(long h, int[] out);
    public static native void close0(long h);
}
