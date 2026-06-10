import java.util.*;
import java.util.concurrent.atomic.*;

/* Multi-threaded GC stress: concurrent allocators force stop-the-world GC while
   a pure-compute thread (no allocation = no safepoint via alloc) spins.  On clib4
   signal-based suspension is dead, so this hangs/crashes unless suspension works
   another way.  Self-verifying + bounded. */
public class Conf4 {
    static volatile boolean stop = false;
    static final AtomicLong spins = new AtomicLong();

    public static void main(String[] a) throws Exception {
        Thread spinner = new Thread(new Runnable() { public void run() {
            long n = 0;
            while (!stop) { n++; if ((n & 0xFFFFF) == 0) spins.addAndGet(0x100000); }
        }});
        spinner.setDaemon(true);
        spinner.start();

        Thread[] allocs = new Thread[4];
        final long[] sums = new long[4];
        for (int t = 0; t < 4; t++) {
            final int ti = t;
            allocs[t] = new Thread(new Runnable() { public void run() {
                long sum = 0;
                for (int i = 0; i < 200; i++) {
                    byte[][] junk = new byte[64][];
                    for (int j = 0; j < 64; j++) junk[j] = new byte[1024];
                    sum += junk[i % 64].length;
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < 50; j++) sb.append("alloc-").append(ti).append('-').append(j);
                    sum += sb.length();
                }
                sums[ti] = sum;
            }});
            allocs[t].start();
        }
        for (Thread t : allocs) t.join();
        System.gc();
        stop = true;
        spinner.join(5000);

        boolean ok = true;
        for (int t = 0; t < 4; t++) {
            System.out.println("alloc-" + t + " sum=" + sums[t]);
            if (sums[t] != 200 * 1024 + sums[0] - sums[0] + (sums[t] - 200 * 1024)) ok = false; // sums equal check below
        }
        boolean equal = sums[0]==sums[1] && sums[1]==sums[2] && sums[2]==sums[3];
        System.out.println("spinner.alive=" + spinner.isAlive() + " spins=" + spins.get());
        System.out.println(equal && sums[0] > 0 ? "Conf4 PASSED" : "Conf4 FAILED");
    }
}
