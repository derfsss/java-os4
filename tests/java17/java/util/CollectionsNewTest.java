package java17.java.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectionsNewTest {
    static final String CLS = "CollectionsNewTest";
    static int P, F;

    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", java.util.Objects.equals(got, exp)); }

    static boolean throwsUOE(Runnable r){
        try { r.run(); return false; }
        catch (UnsupportedOperationException e){ return true; }
        catch (Throwable t){ return false; }
    }

    public static void main(String[] args) {
        try {
            // ---- List.of ----
            List<String> lo = List.of("a", "b", "c");
            ck("List.of size", lo.size(), 3);
            ck("List.of get1", lo.get(1), "b");
            ck("List.of contains", lo.contains("c"), true);
            ck("List.of immutable add throws", throwsUOE(() -> lo.add("z")), true);
            ck("List.of immutable set throws", throwsUOE(() -> lo.set(0, "z")), true);

            // ---- Set.of ----
            Set<Integer> so = Set.of(1, 2, 3, 4);
            ck("Set.of size", so.size(), 4);
            ck("Set.of contains", so.contains(3), true);
            ck("Set.of immutable throws", throwsUOE(() -> so.add(99)), true);

            // ---- Map.of ----
            Map<String, Integer> mo = Map.of("one", 1, "two", 2, "three", 3);
            ck("Map.of size", mo.size(), 3);
            ck("Map.of get", mo.get("two"), 2);
            ck("Map.of immutable put throws", throwsUOE(() -> mo.put("four", 4)), true);

            // ---- Map.ofEntries + Map.entry ----
            Map.Entry<String, Integer> e = Map.entry("k", 7);
            ck("Map.entry getKey", e.getKey(), "k");
            ck("Map.entry getValue", e.getValue(), 7);
            Map<String, Integer> moe = Map.ofEntries(Map.entry("x", 10), Map.entry("y", 20));
            ck("Map.ofEntries get", moe.get("y"), 20);
            ck("Map.ofEntries size", moe.size(), 2);

            // ---- List.copyOf / Set.copyOf / Map.copyOf ----
            List<String> src = new ArrayList<>(List.of("p", "q"));
            List<String> lc = List.copyOf(src);
            src.add("mutated");
            ck("List.copyOf snapshot size", lc.size(), 2);
            ck("List.copyOf immutable throws", throwsUOE(() -> lc.add("z")), true);
            Set<Integer> sc = Set.copyOf(new TreeSet<>(Set.of(5, 6, 7)));
            ck("Set.copyOf contains", sc.contains(6), true);
            ck("Set.copyOf immutable throws", throwsUOE(() -> sc.add(99)), true);
            Map<String, Integer> mc = Map.copyOf(mo);
            ck("Map.copyOf get", mc.get("three"), 3);

            // ---- Collectors.toUnmodifiableList/Set/Map ----
            List<Integer> ul = Stream.of(1, 2, 3).collect(Collectors.toUnmodifiableList());
            ck("toUnmodifiableList value", ul, List.of(1, 2, 3));
            ck("toUnmodifiableList immutable throws", throwsUOE(() -> ul.add(9)), true);
            Set<Integer> us = Stream.of(1, 2, 2, 3).collect(Collectors.toUnmodifiableSet());
            ck("toUnmodifiableSet size", us.size(), 3);
            Map<String, Integer> um = Stream.of("aa", "bbb")
                    .collect(Collectors.toUnmodifiableMap(s -> s, String::length));
            ck("toUnmodifiableMap value", um.get("bbb"), 3);
            ck("toUnmodifiableMap immutable throws", throwsUOE(() -> um.put("c", 1)), true);

            // ---- Optional.ifPresentOrElse / or / stream / isEmpty ----
            AtomicInteger hits = new AtomicInteger(0);
            Optional.of("V").ifPresentOrElse(v -> hits.incrementAndGet(), () -> hits.addAndGet(100));
            ck("Optional.ifPresentOrElse present", hits.get(), 1);
            Optional.empty().ifPresentOrElse(v -> hits.addAndGet(100), () -> hits.addAndGet(10));
            ck("Optional.ifPresentOrElse empty", hits.get(), 11);
            ck("Optional.or present", Optional.of("a").or(() -> Optional.of("b")).get(), "a");
            ck("Optional.or empty", Optional.empty().or(() -> Optional.of("b")).get(), "b");
            ck("Optional.stream present count", Optional.of("z").stream().count(), 1L);
            ck("Optional.stream empty count", Optional.empty().stream().count(), 0L);
            ck("Optional.isEmpty true", Optional.empty().isEmpty(), true);
            ck("Optional.isEmpty false", Optional.of("x").isEmpty(), false);

            // ---- Objects.requireNonNullElse / requireNonNullElseGet ----
            ck("requireNonNullElse non-null", Objects.requireNonNullElse("a", "b"), "a");
            ck("requireNonNullElse null", Objects.requireNonNullElse(null, "b"), "b");
            ck("requireNonNullElseGet non-null", Objects.requireNonNullElseGet("a", () -> "b"), "a");
            ck("requireNonNullElseGet null", Objects.requireNonNullElseGet(null, () -> "b"), "b");

            // ---- Stream.toList() (16) ----
            List<Integer> stl = Stream.of(4, 5, 6).map(i -> i * 2).toList();
            ck("Stream.toList value", stl, List.of(8, 10, 12));
            ck("Stream.toList immutable throws", throwsUOE(() -> stl.add(0)), true);
        }
        catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println(CLS+": "+P+"/"+(P+F)+" passed");
        System.out.println(CLS+" RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
