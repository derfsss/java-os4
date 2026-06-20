package java8.java.util.stream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IntSummaryStatistics;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class StreamTest {
    static int P, F;
    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", java.util.Objects.equals(got, exp)); }

    public static void main(String[] args) {
        try {
            // ---- Stream basics: of, filter, map, collect ----
            List<Integer> mapped = Stream.of(1, 2, 3, 4, 5)
                    .filter(x -> x % 2 == 1)
                    .map(x -> x * 10)
                    .collect(Collectors.toList());
            ck("filter+map+toList", mapped, Arrays.asList(10, 30, 50));

            // iterate + limit
            List<Integer> it = Stream.iterate(1, x -> x * 2).limit(5).collect(Collectors.toList());
            ck("iterate+limit", it, Arrays.asList(1, 2, 4, 8, 16));

            // flatMap
            List<Integer> flat = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5))
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            ck("flatMap", flat, Arrays.asList(1, 2, 3, 4, 5));

            // distinct
            List<Integer> dist = Stream.of(1, 1, 2, 3, 3, 3, 4).distinct().collect(Collectors.toList());
            ck("distinct", dist, Arrays.asList(1, 2, 3, 4));

            // sorted (natural)
            List<Integer> sortNat = Stream.of(5, 3, 1, 4, 2).sorted().collect(Collectors.toList());
            ck("sorted natural", sortNat, Arrays.asList(1, 2, 3, 4, 5));

            // sorted with comparator
            List<Integer> sortRev = Stream.of(5, 3, 1, 4, 2)
                    .sorted(Comparator.reverseOrder()).collect(Collectors.toList());
            ck("sorted(cmp)", sortRev, Arrays.asList(5, 4, 3, 2, 1));

            // peek (side effect) + count
            final int[] seen = {0};
            long peekCount = Stream.of("a", "b", "c").peek(s -> seen[0]++).count();
            // NOTE: in Java 8u count() may not run peek; assert count only, then a peek-with-terminal
            ck("count", peekCount, 3L);
            List<String> peeked = Stream.of("a", "b", "c").peek(s -> seen[0]++).collect(Collectors.toList());
            ck("peek ran on collect", seen[0] >= 3);
            ck("peek passthrough", peeked, Arrays.asList("a", "b", "c"));

            // limit + skip
            List<Integer> skipLimit = Stream.of(0, 1, 2, 3, 4, 5, 6, 7).skip(2).limit(3).collect(Collectors.toList());
            ck("skip+limit", skipLimit, Arrays.asList(2, 3, 4));

            // reduce(identity, op)
            int sumReduce = Stream.of(1, 2, 3, 4).reduce(0, Integer::sum);
            ck("reduce(identity,op)", sumReduce, 10);

            // reduce(op) -> Optional
            Optional<Integer> prod = Stream.of(1, 2, 3, 4).reduce((a, b) -> a * b);
            ck("reduce(op)", prod.get(), 24);

            // anyMatch / allMatch / noneMatch
            ck("anyMatch", Stream.of(1, 2, 3).anyMatch(x -> x == 2), true);
            ck("allMatch", Stream.of(2, 4, 6).allMatch(x -> x % 2 == 0), true);
            ck("noneMatch", Stream.of(1, 3, 5).noneMatch(x -> x % 2 == 0), true);

            // findFirst
            Optional<Integer> first = Stream.of(7, 8, 9).filter(x -> x > 7).findFirst();
            ck("findFirst", first.get(), 8);

            // min / max
            Optional<Integer> min = Stream.of(3, 1, 4, 1, 5).min(Comparator.naturalOrder());
            Optional<Integer> max = Stream.of(3, 1, 4, 1, 5).max(Comparator.naturalOrder());
            ck("min", min.get(), 1);
            ck("max", max.get(), 5);

            // toArray
            Object[] arr = Stream.of("x", "y", "z").toArray();
            ck("toArray length", arr.length, 3);
            ck("toArray[1]", arr[1], "y");
            String[] sarr = Stream.of("x", "y", "z").toArray(String[]::new);
            ck("toArray(gen)", Arrays.asList(sarr), Arrays.asList("x", "y", "z"));

            // mapToInt + sum
            int lenSum = Stream.of("aa", "bbb", "c").mapToInt(String::length).sum();
            ck("mapToInt+sum", lenSum, 6);

            // ---- IntStream ----
            ck("IntStream.range sum", IntStream.range(1, 5).sum(), 10); // 1+2+3+4
            ck("IntStream.rangeClosed sum", IntStream.rangeClosed(1, 5).sum(), 15); // 1..5
            OptionalDouble avg = IntStream.rangeClosed(1, 4).average();
            ck("IntStream.average", avg.getAsDouble(), 2.5);
            OptionalInt imax = IntStream.of(3, 9, 2, 7).max();
            ck("IntStream.max", imax.getAsInt(), 9);
            List<Integer> boxed = IntStream.rangeClosed(1, 3).boxed().collect(Collectors.toList());
            ck("IntStream.boxed", boxed, Arrays.asList(1, 2, 3));
            List<String> i2obj = IntStream.rangeClosed(1, 3).mapToObj(i -> "n" + i).collect(Collectors.toList());
            ck("IntStream.mapToObj", i2obj, Arrays.asList("n1", "n2", "n3"));
            IntSummaryStatistics stats = IntStream.of(2, 4, 6, 8).summaryStatistics();
            ck("IntSummaryStatistics.count", stats.getCount(), 4L);
            ck("IntSummaryStatistics.sum", stats.getSum(), 20L);
            ck("IntSummaryStatistics.min", stats.getMin(), 2);
            ck("IntSummaryStatistics.max", stats.getMax(), 8);
            ck("IntSummaryStatistics.avg", stats.getAverage(), 5.0);
            long asLong = IntStream.rangeClosed(1, 4).asLongStream().sum();
            ck("IntStream.asLongStream.sum", asLong, 10L);

            // ---- LongStream ----
            ck("LongStream.range sum", LongStream.range(1, 5).sum(), 10L);
            ck("LongStream.rangeClosed sum", LongStream.rangeClosed(1, 5).sum(), 15L);
            ck("LongStream.average", LongStream.rangeClosed(1, 4).average().getAsDouble(), 2.5);

            // ---- DoubleStream ----
            double dsum = DoubleStream.of(1.5, 2.5, 3.0).sum();
            ck("DoubleStream.sum", dsum, 7.0);
            ck("DoubleStream.average", DoubleStream.of(2.0, 4.0).average().getAsDouble(), 3.0);
            ck("DoubleStream.max", DoubleStream.of(1.0, 9.0, 4.0).max().getAsDouble(), 9.0);

            // ---- Collectors ----
            Set<Integer> toSet = Stream.of(1, 2, 2, 3).collect(Collectors.toSet());
            ck("Collectors.toSet", toSet, new java.util.HashSet<>(Arrays.asList(1, 2, 3)));

            LinkedList<Integer> toColl = Stream.of(1, 2, 3)
                    .collect(Collectors.toCollection(LinkedList::new));
            ck("Collectors.toCollection", toColl, new LinkedList<>(Arrays.asList(1, 2, 3)));

            TreeSet<Integer> tset = Stream.of(3, 1, 2)
                    .collect(Collectors.toCollection(TreeSet::new));
            ck("toCollection(TreeSet) first", tset.first(), 1);

            Map<String, Integer> toMap = Stream.of("a", "bb", "ccc")
                    .collect(Collectors.toMap(s -> s, String::length));
            ck("Collectors.toMap", toMap.get("bb"), 2);

            String joined = Stream.of("a", "b", "c")
                    .collect(Collectors.joining(",", "[", "]"));
            ck("Collectors.joining", joined, "[a,b,c]");

            Map<Integer, List<String>> grouped = Stream.of("a", "bb", "cc", "ddd")
                    .collect(Collectors.groupingBy(String::length));
            ck("groupingBy len2", grouped.get(2), Arrays.asList("bb", "cc"));

            Map<Integer, Long> groupCount = Stream.of("a", "bb", "cc", "ddd")
                    .collect(Collectors.groupingBy(String::length, Collectors.counting()));
            ck("groupingBy+counting", groupCount.get(2), 2L);

            Map<Boolean, List<Integer>> part = Stream.of(1, 2, 3, 4, 5, 6)
                    .collect(Collectors.partitioningBy(x -> x % 2 == 0));
            ck("partitioningBy even", part.get(true), Arrays.asList(2, 4, 6));
            ck("partitioningBy odd", part.get(false), Arrays.asList(1, 3, 5));

            long cCount = Stream.of("x", "y", "z").collect(Collectors.counting());
            ck("Collectors.counting", cCount, 3L);

            int summed = Stream.of("a", "bb", "ccc").collect(Collectors.summingInt(String::length));
            ck("Collectors.summingInt", summed, 6);

            double avgD = Stream.of(1, 2, 3, 4).collect(Collectors.averagingDouble(x -> (double) x));
            ck("Collectors.averagingDouble", avgD, 2.5);

            List<Integer> mappingColl = Stream.of("a", "bb", "ccc")
                    .collect(Collectors.mapping(String::length, Collectors.toList()));
            ck("Collectors.mapping", mappingColl, Arrays.asList(1, 2, 3));

            int reducedColl = Stream.of(1, 2, 3, 4)
                    .collect(Collectors.reducing(0, Integer::sum));
            ck("Collectors.reducing", reducedColl, 10);

            // groupingBy into a TreeMap-backed result via downstream mapping
            TreeMap<Integer, Long> tm = Stream.of("a", "bb", "cc", "ddd", "ee")
                    .collect(Collectors.groupingBy(String::length, TreeMap::new, Collectors.counting()));
            ck("groupingBy(TreeMap,counting) firstKey", tm.firstKey(), 1);
            ck("groupingBy(TreeMap,counting) len2", tm.get(2), 3L);
        }
        catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println("StreamTest: "+P+"/"+(P+F)+" passed");
        System.out.println("StreamTest RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
