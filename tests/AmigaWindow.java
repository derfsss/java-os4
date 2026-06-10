/* Thin JNI binding for libamigaawt (Phase 4 M3 bring-up test). */
public class AmigaWindow {
    static {
        System.loadLibrary("amigaawt");
    }
    public static native long open0(int w, int h, String title);
    public static native void blit0(long h, int[] px, int x, int y, int w, int hgt, int stride);
    public static native int poll0(long h, int[] out);
    public static native void close0(long h);
}
