package java17.java.util.stream;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class StreamNewTest {
    static final String CLS = "StreamNewTest";
    static int P, F;

    static void ck(String n, boolean ok) {
        if (ok) { P++; System.out.println("[PASS] " + n); }
        else { F++; System.out.println("[FAIL] " + n); }
    }
    static void ck(String n, Object got, Object exp) {
        ck(n + " {got=" + got + " exp=" + exp + "}", Objects.equals(got, exp));
    }

    public static void main(String[] args) {
        try {
            // ---- Stream.takeWhile (Java 9) ----
            ck("takeWhile",
                Stream.of(1, 2, 3, 4, 1, 2).takeWhile(x -> x < 4).collect(Collectors.toList()),
                Arrays.asList(1, 2, 3));
            ck("takeWhile.none",
                Stream.of(5, 6, 7).takeWhile(x -> x < 4).collect(Collectors.toList()),
                Arrays.asList());
            ck("takeWhile.all",
                Stream.of(1, 2, 3).takeWhile(x -> x < 10).collect(Collectors.toList()),
                Arrays.asList(1, 2, 3));

            // ---- Stream.dropWhile (Java 9) ----
            ck("dropWhile",
                Stream.of(1, 2, 3, 4, 1, 2).dropWhile(x -> x < 4).collect(Collectors.toList()),
                Arrays.asList(4, 1, 2));
            ck("dropWhile.all",
                Stream.of(1, 2, 3).dropWhile(x -> x < 10).collect(Collectors.toList()),
                Arrays.asList());
            ck("dropWhile.none",
                Stream.of(5, 6).dropWhile(x -> x < 4).collect(Collectors.toList()),
                Arrays.asList(5, 6));

            // ---- Stream.iterate(seed, hasNext, next) bounded (Java 9) ----
            ck("Stream.iterate.bounded",
                Stream.iterate(1, n -> n <= 16, n -> n * 2).collect(Collectors.toList()),
                Arrays.asList(1, 2, 4, 8, 16));
            ck("Stream.iterate.empty",
                Stream.iterate(10, n -> n < 5, n -> n + 1).collect(Collectors.toList()),
                Arrays.asList());

            // ---- IntStream.iterate(seed, pred, next) (Java 9) ----
            ck("IntStream.iterate.bounded",
                IntStream.iterate(0, i -> i < 10, i -> i + 3).boxed().collect(Collectors.toList()),
                Arrays.asList(0, 3, 6, 9));
            ck("IntStream.iterate.sum",
                IntStream.iterate(1, i -> i <= 5, i -> i + 1).sum(),
                15);

            // ---- Stream.ofNullable (Java 9) ----
            ck("ofNullable.present",
                Stream.ofNullable("x").count(),
                1L);
            ck("ofNullable.null",
                Stream.ofNullable(null).count(),
                0L);

            // ---- Collectors.teeing (Java 12) ----
            // average of 2,4,6,8 = (sum 20)/(count 4) = 5.0
            double avg = Stream.of(2, 4, 6, 8).collect(
                Collectors.teeing(
                    Collectors.summingInt(i -> i),
                    Collectors.counting(),
                    (sum, cnt) -> sum.doubleValue() / cnt));
            ck("teeing.avg", avg, 5.0);

            String teeStr = Stream.of("a", "bb", "ccc").collect(
                Collectors.teeing(
                    Collectors.counting(),
                    Collectors.summingInt(String::length),
                    (cnt, len) -> cnt + ":" + len));
            ck("teeing.combine", teeStr, "3:6");

            // ---- Collectors.filtering (Java 9) ----
            // group by parity, keep only values > 2 within each group
            Map<Integer, List<Integer>> filt = Stream.of(1, 2, 3, 4, 5, 6).collect(
                Collectors.groupingBy(i -> i % 2,
                    Collectors.filtering(i -> i > 2, Collectors.toList())));
            ck("filtering.even", filt.get(0), Arrays.asList(4, 6));
            ck("filtering.odd", filt.get(1), Arrays.asList(3, 5));

            // ---- Collectors.flatMapping (Java 9) ----
            Map<Integer, List<Integer>> flat = Stream.of(
                    Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5))
                .collect(Collectors.groupingBy(
                    List::size,
                    TreeMap::new,
                    Collectors.flatMapping(List::stream, Collectors.toList())));
            ck("flatMapping.size2", flat.get(2), Arrays.asList(1, 2, 3, 4));
            ck("flatMapping.size1", flat.get(1), Arrays.asList(5));

            // ---- Stream.toList (Java 16) ----
            List<Integer> tl = Stream.of(3, 1, 2).sorted().toList();
            ck("toList", tl, Arrays.asList(1, 2, 3));
            // toList result is unmodifiable
            boolean immutable;
            try { tl.add(99); immutable = false; }
            catch (UnsupportedOperationException e) { immutable = true; }
            ck("toList.immutable", immutable, true);

            // ---- combined pipeline ----
            List<Integer> combo = IntStream.iterate(1, i -> i <= 100, i -> i + 1)
                .boxed()
                .takeWhile(i -> i < 50)
                .dropWhile(i -> i < 47)
                .toList();
            ck("combo", combo, Arrays.asList(47, 48, 49));
        }
        catch (Throwable t) {
            F++; System.out.println("[FAIL] threw " + t);
        }
        System.out.println(CLS + ": " + P + "/" + (P + F) + " passed");
        System.out.println(CLS + " RESULT: " + (F == 0 ? "PASS" : "FAIL"));
        System.exit(F == 0 ? 0 : 1);
    }
}
