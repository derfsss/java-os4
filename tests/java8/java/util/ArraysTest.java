package java8.java.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class ArraysTest {
    static final String CLS = "ArraysTest";
    static int P, F;

    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", java.util.Objects.equals(got, exp)); }

    public static void main(String[] args) {
        try {
            // ---- Arrays.sort primitives ----
            int[] ints = {5, 3, 9, 1, 4};
            Arrays.sort(ints);
            ck("sort int[]", Arrays.toString(ints), "[1, 3, 4, 5, 9]");

            // ---- Arrays.sort objects (natural) ----
            String[] strs = {"banana", "apple", "cherry"};
            Arrays.sort(strs);
            ck("sort String[]", Arrays.toString(strs), "[apple, banana, cherry]");

            // ---- Arrays.sort with Comparator ----
            String[] byLen = {"ccc", "a", "bb"};
            Arrays.sort(byLen, Comparator.comparingInt(String::length));
            ck("sort with Comparator", Arrays.toString(byLen), "[a, bb, ccc]");

            String[] rev = {"a", "b", "c"};
            Arrays.sort(rev, Comparator.reverseOrder());
            ck("sort reverseOrder", Arrays.toString(rev), "[c, b, a]");

            // ---- Arrays.binarySearch ----
            int[] sorted = {1, 3, 4, 5, 9};
            ck("binarySearch found", Arrays.binarySearch(sorted, 5), 3);
            ck("binarySearch missing", Arrays.binarySearch(sorted, 2), -2); // insertion point 1 -> -(1)-1

            // ---- Arrays.copyOf ----
            int[] grown = Arrays.copyOf(new int[]{7, 8}, 4);
            ck("copyOf grow", Arrays.toString(grown), "[7, 8, 0, 0]");
            int[] shrunk = Arrays.copyOf(new int[]{7, 8, 9}, 2);
            ck("copyOf shrink", Arrays.toString(shrunk), "[7, 8]");

            // ---- Arrays.copyOfRange ----
            int[] range = Arrays.copyOfRange(new int[]{10, 20, 30, 40}, 1, 3);
            ck("copyOfRange", Arrays.toString(range), "[20, 30]");

            // ---- Arrays.fill ----
            int[] filled = new int[3];
            Arrays.fill(filled, 7);
            ck("fill", Arrays.toString(filled), "[7, 7, 7]");

            // ---- Arrays.equals ----
            ck("equals true", Arrays.equals(new int[]{1, 2, 3}, new int[]{1, 2, 3}), true);
            ck("equals false", Arrays.equals(new int[]{1, 2, 3}, new int[]{1, 2, 4}), false);

            // ---- Arrays.deepEquals ----
            int[][] d1 = {{1, 2}, {3, 4}};
            int[][] d2 = {{1, 2}, {3, 4}};
            ck("deepEquals", Arrays.deepEquals(d1, d2), true);
            ck("equals shallow on nested false", Arrays.equals(d1, d2), false);

            // ---- Arrays.hashCode ----
            ck("hashCode matches", Arrays.hashCode(new int[]{1, 2, 3}), Arrays.hashCode(new int[]{1, 2, 3}));

            // ---- Arrays.asList ----
            List<String> al = Arrays.asList("x", "y", "z");
            ck("asList size", al.size(), 3);
            ck("asList get", al.get(1), "y");

            // ---- Arrays.stream ----
            int sum = Arrays.stream(new int[]{1, 2, 3, 4}).sum();
            ck("stream sum", sum, 10);

            // ---- Arrays.deepToString ----
            ck("deepToString", Arrays.deepToString(new int[][]{{1, 2}, {3}}), "[[1, 2], [3]]");

            // ---- Arrays.setAll ----
            int[] sa = new int[4];
            Arrays.setAll(sa, i -> i * i);
            ck("setAll", Arrays.toString(sa), "[0, 1, 4, 9]");

            // ---- System.arraycopy ----
            int[] src = {1, 2, 3, 4, 5};
            int[] dst = new int[5];
            System.arraycopy(src, 1, dst, 0, 3);
            ck("System.arraycopy", Arrays.toString(dst), "[2, 3, 4, 0, 0]");

            // ---- Optional ----
            Optional<String> opt = Optional.of("hi");
            ck("Optional.isPresent", opt.isPresent(), true);
            ck("Optional.get", opt.get(), "hi");

            Optional<String> empty = Optional.empty();
            ck("Optional.empty isPresent", empty.isPresent(), false);
            ck("Optional.orElse", empty.orElse("def"), "def");
            ck("Optional.orElseGet", empty.orElseGet(() -> "gen"), "gen");

            ck("Optional.ofNullable null", Optional.ofNullable(null).isPresent(), false);
            ck("Optional.ofNullable val", Optional.ofNullable("v").get(), "v");

            ck("Optional.map", Optional.of("abc").map(String::length).get(), 3);
            ck("Optional.filter keep", Optional.of(4).filter(x -> x > 2).get(), 4);
            ck("Optional.filter drop", Optional.of(1).filter(x -> x > 2).isPresent(), false);
            ck("Optional.flatMap", Optional.of("q").flatMap(s -> Optional.of(s + "!")).get(), "q!");

            AtomicInteger seen = new AtomicInteger(0);
            Optional.of(5).ifPresent(v -> seen.set(v));
            ck("Optional.ifPresent", seen.get(), 5);

            // ---- Objects ----
            ck("Objects.equals true", Objects.equals("a", "a"), true);
            ck("Objects.equals null both", Objects.equals(null, null), true);
            ck("Objects.deepEquals", Objects.deepEquals(new int[]{1, 2}, new int[]{1, 2}), true);
            ck("Objects.hash deterministic", Objects.hash(1, "a"), Objects.hash(1, "a"));
            ck("Objects.hashCode null", Objects.hashCode(null), 0);
            ck("Objects.toString default", Objects.toString(null, "fb"), "fb");
            ck("Objects.isNull", Objects.isNull(null), true);
            ck("Objects.nonNull", Objects.nonNull("x"), true);

            boolean threw = false;
            try { Objects.requireNonNull(null, "must not be null"); }
            catch (NullPointerException e) { threw = true; }
            ck("Objects.requireNonNull throws", threw, true);

        } catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println(CLS+": "+P+"/"+(P+F)+" passed");
        System.out.println(CLS+" RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
