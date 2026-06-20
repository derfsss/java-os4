/*
 * VersionGateTest -- regression test for the class-file version gate (fix #5).
 *
 * A class compiled for Java 9+ (class-file major >= 53) must be rejected at load
 * with UnsupportedClassVersionError, instead of loading and then dying far away
 * with a BootstrapMethodError (the "it just freezes" failure mode).
 *
 * Self-contained: reads this class's OWN bytes (a valid Java-8 / major-52 class),
 * bumps the class-file major version to 53 in memory, and asks a fresh
 * ClassLoader to defineClass() it.  The VM's gate fires in parseClass (before any
 * name check), so this needs no separate fixture and does not depend on the app
 * classloader's findClass (which made the older Class.forName approach flaky on
 * the Amiga toolkit).
 *
 * Run:  jamvm-openjdk -cp testsuite.zip VersionGateTest
 *
 * GPLv2 (java-os4 project).
 */
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class VersionGateTest {

    /** exposes defineClass so we can hand the VM raw (patched) class bytes */
    static final class Defining extends ClassLoader {
        Class<?> def(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }

    public static void main(String[] args) {
        boolean pass = false;
        byte[] cls = readSelf();

        if (cls == null) {
            System.out.println("[INFO] could not read own class bytes; fall back"
                + " to: jamvm-openjdk -cp testsuite.zip V9Bomb (a prebuilt"
                + " major-53 class) -> expect UnsupportedClassVersionError");
        } else {
            int major = ((cls[6] & 0xFF) << 8) | (cls[7] & 0xFF);
            System.out.println("[INFO] own class-file major=" + major
                + "; patching to 53 (Java 9) and defining it");
            cls[6] = 0;
            cls[7] = 53;                       /* major 52 -> 53 */
            try {
                Class<?> c = new Defining().def(
                    VersionGateTest.class.getName(), cls);
                System.out.println("[FAIL] a major-53 class LOADED (" + c
                    + ") -- the version gate did not fire");
            } catch (UnsupportedClassVersionError e) {
                pass = true;
                System.out.println("[PASS] version gate rejected major 53:");
                System.out.println("       " + e.getMessage());
            } catch (Throwable t) {
                System.out.println("[FAIL] expected UnsupportedClassVersionError,"
                    + " got " + t);
            }
        }

        System.out.println("VersionGateTest RESULT: " + (pass ? "PASS" : "FAIL"));
        System.out.println("VersionGateTest DONE");
        System.exit(pass ? 0 : 1);
    }

    private static byte[] readSelf() {
        String res = "/" + VersionGateTest.class.getName().replace('.', '/')
                   + ".class";
        try (InputStream in = VersionGateTest.class.getResourceAsStream(res)) {
            if (in == null)
                return null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0)
                bos.write(buf, 0, n);
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}
