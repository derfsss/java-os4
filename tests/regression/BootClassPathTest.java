/*
 * BootClassPathTest -- regression test for the AmigaOS sun.boot.class.path
 * separator fix (forum 0.5.3: the "Please insert volume niopatch.zip" requester
 * and the "Trampoline must not be defined by the bootstrap class loader" Error).
 *
 * Root cause (pre-fix): JamVM exposed sun.boot.class.path as its raw ':'-joined C
 * boot path ("niopatch.zip:resources.jar:rt.jar:..."), because its own boot-path
 * parser splits on ':'.  But on AmigaOS path.separator is ';' (':' is the volume
 * separator), and OpenJDK's sun.misc.Launcher.getBootstrapClassPath() splits
 * sun.boot.class.path on path.separator -- so the whole value was read as ONE bogus
 * entry "niopatch.zip:resources.jar:...".  AmigaDOS treated the leading
 * "niopatch.zip:" as a missing volume (the requester), and every
 * getBootstrapResource() then failed.  That broke sun.reflect.misc.MethodUtil:
 * unable to read sun/reflect/misc/Trampoline.class via the (broken) boot
 * URLClassPath, it fell back to the bootstrap loader, which defined Trampoline with
 * the null loader, so Trampoline.<clinit> threw the Error -- first hit running
 * SwingDemo / VmSuite, the moment any code bounces a reflective call through
 * MethodUtil (java.beans, AWT event dispatch, JMX, ...).
 *
 * The fix exposes sun.boot.class.path as ';'-separated ABSOLUTE Unix-form entries
 * anchored at java.home, so it splits correctly and every boot jar resolves.
 *
 * This test fails on the UNFIXED VM (CHECK 1: one mega-entry; CHECK 2:
 * getSystemResource == null + the volume requester; CHECK 3: the Trampoline Error)
 * and passes on the fixed VM.  Portable: on a correct desktop JVM all three pass,
 * so it is also "green on a correct JVM" per the suite's gate philosophy.
 *
 * Run:  jamvm-openjdk -cp testsuite.zip BootClassPathTest
 *
 * GPLv2 (java-os4 project).
 */
import java.net.URL;
import java.util.regex.Pattern;

public class BootClassPathTest {
    static int fails = 0;

    static void ck(String name, boolean ok) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
        if (!ok) fails++;
    }

    /** how many ".jar"/".zip" archive names one path-list entry mentions */
    static int archiveCount(String s) {
        String low = s.toLowerCase();
        int n = 0, i = 0;
        while ((i = low.indexOf(".jar", i)) >= 0) { n++; i += 4; }
        for (i = 0; (i = low.indexOf(".zip", i)) >= 0; i += 4) n++;
        return n;
    }

    public static void main(String[] args) {
        String sep  = System.getProperty("path.separator");
        String boot = System.getProperty("sun.boot.class.path");
        System.out.println("[INFO] path.separator=[" + sep + "]");
        System.out.println("[INFO] sun.boot.class.path=[" + boot + "]");

        // CHECK 1 -- sun.boot.class.path must use path.separator between entries, so
        // it splits into the individual boot archives.  Pre-fix on AmigaOS it is
        // ':'-joined under a ';' path.separator => exactly ONE entry naming every
        // boot jar at once.
        if (boot == null || boot.isEmpty()) {
            // sun.boot.class.path is a Java-8 property; absent on 9+ (where this VM
            // does not run anyway).  Don't fail the separator check there.
            System.out.println("[INFO] sun.boot.class.path empty/null (Java 9+?)"
                + " -- separator checks N/A on this JVM");
        } else {
            String[] parts = boot.split(Pattern.quote(sep), -1);
            ck("sun.boot.class.path splits into >1 entry on path.separator (got "
                + parts.length + ")", parts.length > 1);
            int worst = 0;
            for (String p : parts) worst = Math.max(worst, archiveCount(p));
            ck("no single boot entry names more than one archive (max=" + worst
                + ") -- i.e. it was not joined with the wrong separator", worst <= 1);
        }

        // CHECK 2 -- a boot-classpath RESOURCE must resolve.  getSystemResource for a
        // core class delegates to getBootstrapResource, which builds a URLClassPath
        // from sun.boot.class.path.  Pre-fix on AmigaOS this popped the
        // "Please insert volume niopatch.zip" requester and returned null.
        URL u = ClassLoader.getSystemResource("java/lang/Object.class");
        System.out.println("[INFO] getSystemResource(java/lang/Object.class)=" + u);
        ck("bootstrap resource java/lang/Object.class resolves (no volume requester)",
            u != null);

        // CHECK 3 -- the exact forum crash path: java.beans.Expression bounces a
        // reflective call through sun.reflect.misc.MethodUtil, whose static init
        // loads sun.reflect.misc.Trampoline via its OWN loader.  When
        // getBootstrapResource is broken, MethodUtil cannot read the Trampoline bytes,
        // falls back to the bootstrap loader, and Trampoline.<clinit> throws
        // "Trampoline must not be defined by the bootstrap class loader".
        boolean trampOk = false;
        Object result = null;
        try {
            java.beans.Expression ex =
                new java.beans.Expression("hello", "length", new Object[0]);
            result = ex.getValue();                 // -> MethodUtil.invoke -> Trampoline
            trampOk = Integer.valueOf(5).equals(result);
        } catch (Throwable t) {
            System.out.println("[INFO] MethodUtil/Trampoline path threw: " + t);
        }
        ck("java.beans.Expression via MethodUtil/Trampoline returns 5 (got "
            + result + ")", trampOk);

        boolean pass = fails == 0;
        System.out.println("BootClassPathTest RESULT: " + (pass ? "PASS" : "FAIL"));
        System.out.println("BootClassPathTest DONE");
        System.exit(pass ? 0 : 1);
    }
}
