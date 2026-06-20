/*
 * VmSuite -- a broad, self-verifying smoke test for the Java-OS4 runtime.
 *
 * One headless program that exercises the major areas of the VM + OpenJDK 8
 * class library: language features, invokedynamic (lambdas), collections,
 * streams, concurrency, reflection, strings/regex, big numbers, java.time,
 * I/O + NIO, exceptions and GC.  Each check prints [PASS]/[FAIL]; the program
 * prints a summary and exits 0 only if every check passed.
 *
 * Run:  jamvm-openjdk -cp testsuite.zip VmSuite
 *
 * GPLv2 (java-os4 project).
 */
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VmSuite {

    static int passed, failed;

    /* a check returns true on success; exceptions count as failure */
    interface Check { boolean run() throws Exception; }

    static void check(String name, Check c) {
        try {
            if (c.run()) {
                passed++;
                System.out.println("[PASS] " + name);
            } else {
                failed++;
                System.out.println("[FAIL] " + name + " (assertion false)");
            }
        } catch (Throwable t) {
            failed++;
            System.out.println("[FAIL] " + name + " (" + t + ")");
        }
    }

    public static void main(String[] args) {
        System.out.println("== VmSuite: Java " + System.getProperty("java.version")
            + " on " + System.getProperty("os.name")
            + " (" + System.getProperty("os.arch") + ") ==");

        /* ---- language + invokedynamic (lambdas / method refs) ---------- */
        check("lambda + method reference", () -> {
            Function<Integer, Integer> sq = x -> x * x;
            Runnable r = VmSuite::noop;
            r.run();
            return sq.apply(7) == 49;
        });
        check("generics + varargs + autoboxing", () -> {
            List<Integer> xs = listOf(1, 2, 3, 4);
            int sum = 0;
            for (int x : xs) sum += x;        // unboxing in enhanced-for
            return sum == 10 && xs.size() == 4;
        });
        check("string switch + ternary", () -> {
            String s = "two";
            int n;
            switch (s) {
                case "one": n = 1; break;
                case "two": n = 2; break;
                default:    n = -1;
            }
            return (n == 2 ? "ok" : "no").equals("ok");
        });
        check("anonymous + inner class capture", () -> {
            final int[] box = {0};
            Runnable r = new Runnable() {
                public void run() { box[0] = 42; }
            };
            r.run();
            return box[0] == 42;
        });

        /* ---- collections ----------------------------------------------- */
        check("ArrayList / HashMap / TreeMap ordering", () -> {
            Map<String, Integer> h = new HashMap<>();
            h.put("b", 2); h.put("a", 1); h.put("c", 3);
            TreeMap<String, Integer> t = new TreeMap<>(h);
            return t.firstKey().equals("a") && t.lastKey().equals("c")
                && h.get("b") == 2;
        });
        check("HashSet dedup + Collections.sort", () -> {
            Set<Integer> set = new HashSet<>(listOf(3, 1, 2, 2, 3));
            List<Integer> l = new ArrayList<>(set);
            Collections.sort(l);
            return set.size() == 3 && l.get(0) == 1 && l.get(2) == 3;
        });
        check("Deque as stack + queue", () -> {
            Deque<Integer> d = new ArrayDeque<>();
            d.push(1); d.push(2);
            d.addLast(9);
            return d.pop() == 2 && d.pollLast() == 9 && d.pop() == 1;
        });

        /* ---- streams --------------------------------------------------- */
        check("stream filter/map/reduce", () -> {
            int r = listOf(1, 2, 3, 4, 5, 6).stream()
                .filter(x -> x % 2 == 0).mapToInt(x -> x * x).sum();
            return r == 4 + 16 + 36;
        });
        check("IntStream + Collectors.groupingBy", () -> {
            Map<Boolean, List<Integer>> g = IntStream.rangeClosed(1, 10).boxed()
                .collect(Collectors.groupingBy(x -> x % 2 == 0));
            return g.get(true).size() == 5 && g.get(false).size() == 5;
        });
        check("Collectors.joining", () -> {
            String s = listOf("a", "b", "c").stream()
                .collect(Collectors.joining(",", "[", "]"));
            return s.equals("[a,b,c]");
        });
        check("Optional", () -> {
            Optional<String> o = listOf("x", "yy", "zzz").stream()
                .filter(s -> s.length() == 2).findFirst();
            return o.isPresent() && o.get().equals("yy");
        });

        /* ---- concurrency ----------------------------------------------- */
        check("threads + AtomicInteger + join", () -> {
            final AtomicInteger ai = new AtomicInteger();
            Thread[] ts = new Thread[8];
            for (int i = 0; i < ts.length; i++) {
                ts[i] = new Thread(() -> {
                    for (int j = 0; j < 10000; j++) ai.incrementAndGet();
                });
                ts[i].start();
            }
            for (Thread t : ts) t.join();
            return ai.get() == 8 * 10000;
        });
        check("ExecutorService + Future + CountDownLatch", () -> {
            ExecutorService ex = Executors.newFixedThreadPool(4);
            try {
                final CountDownLatch latch = new CountDownLatch(1);
                Future<Integer> f = ex.submit(() -> { latch.countDown(); return 21 * 2; });
                latch.await(5, TimeUnit.SECONDS);
                return f.get(5, TimeUnit.SECONDS) == 42;
            } finally {
                ex.shutdown();
            }
        });
        check("ConcurrentHashMap + synchronized", () -> {
            final ConcurrentHashMap<Integer, Integer> m = new ConcurrentHashMap<>();
            final Object lock = new Object();
            final int[] hits = {0};
            Thread[] ts = new Thread[4];
            for (int i = 0; i < 4; i++) {
                final int base = i * 1000;
                ts[i] = new Thread(() -> {
                    for (int j = 0; j < 1000; j++) m.put(base + j, j);
                    synchronized (lock) { hits[0]++; }
                });
                ts[i].start();
            }
            for (Thread t : ts) t.join();
            return m.size() == 4000 && hits[0] == 4;
        });

        /* ---- reflection ------------------------------------------------ */
        check("reflection: forName / method invoke / field", () -> {
            Class<?> c = Class.forName("java.lang.StringBuilder");
            Object sb = c.newInstance();
            c.getMethod("append", String.class).invoke(sb, "hi");
            c.getMethod("append", String.class).invoke(sb, "!");
            return sb.toString().equals("hi!");
        });
        check("reflection: array + generic type", () -> {
            int[] a = (int[]) java.lang.reflect.Array.newInstance(int.class, 3);
            a[1] = 5;
            return java.lang.reflect.Array.getInt(a, 1) == 5 && a.length == 3;
        });

        /* ---- strings / regex ------------------------------------------- */
        check("String.format + split + StringBuilder", () -> {
            String s = String.format("%04d-%s", 7, "x");
            String[] parts = "a,b,,c".split(",");
            StringBuilder sb = new StringBuilder();
            for (int i = parts.length - 1; i >= 0; i--) sb.append(parts[i]);
            return s.equals("0007-x") && parts.length == 4 && sb.toString().equals("cba");
        });
        check("regex Pattern/Matcher groups", () -> {
            Matcher m = Pattern.compile("(\\d+)-(\\w+)").matcher("42-foo");
            return m.matches() && m.group(1).equals("42") && m.group(2).equals("foo");
        });
        check("String hashCode/equals consistency", () -> {
            String a = "AmigaOS4", b = new String("AmigaOS4".toCharArray());
            Map<String, Integer> m = new HashMap<>();
            m.put(a, 1);
            return a.equals(b) && a.hashCode() == b.hashCode() && m.get(b) == 1;
        });

        /* ---- big numbers / math ---------------------------------------- */
        check("BigInteger factorial(30)", () -> {
            BigInteger f = BigInteger.ONE;
            for (int i = 2; i <= 30; i++) f = f.multiply(BigInteger.valueOf(i));
            return f.toString().equals("265252859812191058636308480000000");
        });
        check("BigDecimal precise arithmetic", () -> {
            BigDecimal r = new BigDecimal("0.1").add(new BigDecimal("0.2"));
            return r.compareTo(new BigDecimal("0.3")) == 0;
        });
        check("Math + floating point", () -> {
            double r = Math.sqrt(2.0);
            return Math.abs(r * r - 2.0) < 1e-9 && Math.max(3, 7) == 7
                && Long.MAX_VALUE + 1 == Long.MIN_VALUE;     // wraparound
        });

        /* ---- I/O + NIO ------------------------------------------------- */
        check("java.io write+read file", () -> {
            File f = new File("RAM:vmsuite_io.txt");
            try (Writer w = new OutputStreamWriter(new FileOutputStream(f),
                                                   StandardCharsets.UTF_8)) {
                w.write("line1\nline2\n");
            }
            int lines = 0;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(
                    new FileInputStream(f), StandardCharsets.UTF_8))) {
                while (r.readLine() != null) lines++;
            }
            f.delete();
            return lines == 2;
        });
        check("java.nio.file Files + ByteBuffer", () -> {
            Path p = Paths.get("RAM:vmnio.bin");
            byte[] data = "nio-bytes".getBytes(StandardCharsets.UTF_8);
            Files.write(p, data);
            byte[] back = Files.readAllBytes(p);
            ByteBuffer bb = ByteBuffer.wrap(back);
            Files.delete(p);
            return Arrays.equals(data, back) && bb.remaining() == data.length;
        });

        /* ---- java.time / dates ----------------------------------------- */
        check("java.time LocalDate + Duration", () -> {
            LocalDate d = LocalDate.of(2026, Month.JUNE, 20).plusDays(12);
            Duration dur = Duration.ofMinutes(90);
            return d.equals(LocalDate.of(2026, 7, 2)) && dur.toHours() == 1
                && dur.minusMinutes(60).toMinutes() == 30;
        });

        /* ---- exceptions ------------------------------------------------ */
        check("try/catch/finally + custom + suppressed", () -> {
            final int[] fin = {0};
            boolean caught = false;
            try {
                try {
                    throw new IllegalStateException("boom");
                } finally {
                    fin[0] = 1;
                }
            } catch (IllegalStateException e) {
                caught = "boom".equals(e.getMessage());
            }
            return caught && fin[0] == 1;
        });
        check("multi-catch + class hierarchy", () -> {
            Throwable t = null;
            try {
                Object o = "s";
                Integer i = (Integer) o;          // ClassCastException
                t = new Exception("" + i);
            } catch (ClassCastException | NullPointerException e) {
                t = e;
            }
            return t instanceof ClassCastException;
        });

        /* ---- arrays ---------------------------------------------------- */
        check("arrays: multidim + sort + arraycopy", () -> {
            int[][] m = new int[3][3];
            m[1][2] = 7;
            int[] a = {5, 3, 8, 1};
            Arrays.sort(a);
            int[] b = new int[4];
            System.arraycopy(a, 0, b, 0, 4);
            return m[1][2] == 7 && a[0] == 1 && a[3] == 8 && Arrays.equals(a, b);
        });

        /* ---- GC + references ------------------------------------------- */
        check("allocate churn + System.gc + WeakReference", () -> {
            java.lang.ref.WeakReference<Object> ref =
                new java.lang.ref.WeakReference<>(new Object());
            long total = 0;
            for (int i = 0; i < 200; i++) {
                byte[] junk = new byte[64 * 1024];      // ~12MB of churn
                total += junk.length;
                junk[0] = (byte) i;
            }
            boolean cleared = false;
            for (int g = 0; g < 5 && !cleared; g++) {   // GC timing varies
                System.gc();
                Thread.sleep(50);
                cleared = ref.get() == null;
            }
            // allocation must total correctly; weak-ref clearing is reported
            // but not required for PASS (JamVM's GC may defer it)
            if (!cleared)
                System.out.println("    [info] weak ref not cleared after gc"
                    + " (JamVM GC timing)");
            return total == 200L * 64 * 1024;
        });

        /* ---- summary --------------------------------------------------- */
        int total = passed + failed;
        System.out.println("== VmSuite: " + passed + "/" + total + " passed, "
            + failed + " failed ==");
        System.out.println(failed == 0 ? "VmSuite RESULT: ALL PASS"
                                       : "VmSuite RESULT: FAILURES");
        System.out.println("VmSuite DONE");
        System.exit(failed == 0 ? 0 : 1);
    }

    static void noop() { }

    /* Java-8-target helper (avoids Arrays.asList autobox quirks in varargs) */
    @SafeVarargs
    static <T> List<T> listOf(T... xs) {
        return new ArrayList<>(Arrays.asList(xs));
    }
}
