/*
 * VersionGateTest -- regression test for the class-file version gate (fix #5).
 *
 * A class compiled for Java 9+ (class-file major 53+) must be rejected at load
 * with UnsupportedClassVersionError, instead of loading and then dying far away
 * with a mysterious BootstrapMethodError (the "it just freezes" failure mode).
 *
 * Loads V9Bomb -- a normal class whose major version was bumped 52 -> 53 -- from
 * testsuite.zip and asserts the gate fires.  (Catches the error on stdout so the
 * result is captured regardless of stderr handling.)
 *
 * Run:  jamvm-openjdk -cp testsuite.zip VersionGateTest
 *
 * GPLv2 (java-os4 project).
 */
public class VersionGateTest {
    public static void main(String[] args) {
        boolean pass = false;
        try {
            Class<?> c = Class.forName("V9Bomb");
            System.out.println("[FAIL] V9Bomb (class file v53) LOADED -"
                + " the version gate did not fire: " + c);
        } catch (UnsupportedClassVersionError e) {
            pass = true;
            System.out.println("[PASS] version gate rejected V9Bomb:");
            System.out.println("       " + e.getMessage());
        } catch (Throwable t) {
            System.out.println("[FAIL] V9Bomb load threw " + t
                + " (expected UnsupportedClassVersionError)");
        }
        System.out.println("VersionGateTest RESULT: " + (pass ? "PASS" : "FAIL"));
        System.out.println("VersionGateTest DONE");
        System.exit(pass ? 0 : 1);
    }
}
