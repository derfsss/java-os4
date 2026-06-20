package java8.java.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

public class CollectionsTest {

    static final String CLS = "CollectionsTest";
    static int P, F;

    static void ck(String n, boolean ok) {
        if (ok) { P++; System.out.println("[PASS] " + n); }
        else { F++; System.out.println("[FAIL] " + n); }
    }

    static void ck(String n, Object got, Object exp) {
        ck(n + " {got=" + got + " exp=" + exp + "}", java.util.Objects.equals(got, exp));
    }

    public static void main(String[] args) {
        try {
            // --- ArrayList ---
            List<Integer> al = new ArrayList<>(Arrays.asList(3, 1, 2));
            al.add(4);
            ck("ArrayList.size", al.size(), 4);
            ck("ArrayList.get", al.get(0), 3);
            ck("ArrayList.indexOf", al.indexOf(2), 2);

            // --- LinkedList (as deque) ---
            LinkedList<String> ll = new LinkedList<>();
            ll.addFirst("b");
            ll.addFirst("a");
            ll.addLast("c");
            ck("LinkedList.getFirst", ll.getFirst(), "a");
            ck("LinkedList.getLast", ll.getLast(), "c");
            ck("LinkedList.peek", ll.peek(), "a");

            // --- Vector ---
            Vector<Integer> v = new Vector<>();
            v.add(10);
            v.add(20);
            ck("Vector.elementAt", v.elementAt(1), 20);
            ck("Vector.firstElement", v.firstElement(), 10);

            // --- Stack ---
            Stack<Integer> st = new Stack<>();
            st.push(1);
            st.push(2);
            ck("Stack.peek", st.peek(), 2);
            ck("Stack.pop", st.pop(), 2);
            ck("Stack.search", st.search(1), 1);

            // --- ArrayDeque as stack + queue ---
            Deque<Integer> ad = new ArrayDeque<>();
            ad.push(1);
            ad.push(2);            // stack: top=2
            ck("ArrayDeque.peekStack", ad.peek(), 2);
            ad.offer(9);           // queue tail
            ck("ArrayDeque.pollHead", ad.poll(), 2);
            ck("ArrayDeque.pollLast", ad.pollLast(), 9);

            // --- PriorityQueue (natural ordering) ---
            PriorityQueue<Integer> pq = new PriorityQueue<>();
            pq.add(5);
            pq.add(1);
            pq.add(3);
            ck("PriorityQueue.peekMin", pq.peek(), 1);
            ck("PriorityQueue.pollMin", pq.poll(), 1);
            ck("PriorityQueue.pollNext", pq.poll(), 3);

            // --- HashMap ---
            HashMap<String, Integer> hm = new HashMap<>();
            hm.put("a", 1);
            hm.put("b", 2);
            ck("HashMap.get", hm.get("b"), 2);
            ck("HashMap.getOrDefault", hm.getOrDefault("z", -1), -1);
            ck("HashMap.containsKey", hm.containsKey("a"), true);

            // --- LinkedHashMap (insertion order) ---
            LinkedHashMap<String, Integer> lhm = new LinkedHashMap<>();
            lhm.put("z", 1);
            lhm.put("a", 2);
            lhm.put("m", 3);
            ck("LinkedHashMap.keyOrder", new ArrayList<>(lhm.keySet()),
                    Arrays.asList("z", "a", "m"));

            // --- TreeMap (navigable) ---
            TreeMap<Integer, String> tm = new TreeMap<>();
            tm.put(10, "x");
            tm.put(20, "y");
            tm.put(30, "z");
            ck("TreeMap.firstKey", tm.firstKey(), 10);
            ck("TreeMap.lastKey", tm.lastKey(), 30);
            ck("TreeMap.floorKey", tm.floorKey(25), 20);
            ck("TreeMap.ceilingKey", tm.ceilingKey(25), 30);
            ck("TreeMap.headMapKeys", new ArrayList<>(tm.headMap(30).keySet()),
                    Arrays.asList(10, 20));
            ck("TreeMap.tailMapKeys", new ArrayList<>(tm.tailMap(20).keySet()),
                    Arrays.asList(20, 30));

            // --- Hashtable ---
            Hashtable<String, Integer> ht = new Hashtable<>();
            ht.put("k", 42);
            ck("Hashtable.get", ht.get("k"), 42);
            ck("Hashtable.size", ht.size(), 1);

            // --- HashSet ---
            HashSet<Integer> hs = new HashSet<>(Arrays.asList(1, 2, 2, 3));
            ck("HashSet.size", hs.size(), 3);
            ck("HashSet.contains", hs.contains(2), true);

            // --- LinkedHashSet (insertion order) ---
            LinkedHashSet<String> lhs = new LinkedHashSet<>();
            lhs.add("c");
            lhs.add("a");
            lhs.add("b");
            lhs.add("a");
            ck("LinkedHashSet.order", new ArrayList<>(lhs),
                    Arrays.asList("c", "a", "b"));

            // --- TreeSet (sorted + navigable) ---
            TreeSet<Integer> ts = new TreeSet<>(Arrays.asList(5, 1, 3, 9));
            ck("TreeSet.first", ts.first(), 1);
            ck("TreeSet.last", ts.last(), 9);
            ck("TreeSet.lower", ts.lower(5), 3);
            ck("TreeSet.higher", ts.higher(5), 9);
            ck("TreeSet.headSet", new ArrayList<>(ts.headSet(5)),
                    Arrays.asList(1, 3));

            // --- Collections ---
            List<Integer> cs = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5));
            Collections.sort(cs);
            ck("Collections.sort", cs, Arrays.asList(1, 1, 3, 4, 5));
            Collections.reverse(cs);
            ck("Collections.reverse", cs, Arrays.asList(5, 4, 3, 1, 1));
            ck("Collections.max", Collections.max(cs), 5);
            ck("Collections.min", Collections.min(cs), 1);
            ck("Collections.frequency", Collections.frequency(cs, 1), 2);

            List<Integer> sorted = Arrays.asList(1, 3, 5, 7, 9);
            ck("Collections.binarySearch", Collections.binarySearch(sorted, 7), 3);

            List<Integer> base = Collections.unmodifiableList(Arrays.asList(1, 2, 3));
            boolean threw = false;
            try { base.add(4); } catch (UnsupportedOperationException e) { threw = true; }
            ck("Collections.unmodifiableList", threw, true);

            ck("Collections.emptyList", Collections.emptyList(), Arrays.asList());
            ck("Collections.singletonList", Collections.singletonList("x"),
                    Arrays.asList("x"));
            ck("Collections.nCopies", Collections.nCopies(3, "q"),
                    Arrays.asList("q", "q", "q"));

            List<Integer> addTo = new ArrayList<>(Arrays.asList(0));
            Collections.addAll(addTo, 1, 2, 3);
            ck("Collections.addAll", addTo, Arrays.asList(0, 1, 2, 3));

            // --- Comparator ---
            List<String> words = new ArrayList<>(Arrays.asList("bb", "a", "ccc", "dd"));
            words.sort(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));
            ck("Comparator.comparing+thenComparing", words,
                    Arrays.asList("a", "bb", "dd", "ccc"));

            List<Integer> nums = new ArrayList<>(Arrays.asList(1, 2, 3));
            nums.sort(Comparator.<Integer>naturalOrder().reversed());
            ck("Comparator.reversed", nums, Arrays.asList(3, 2, 1));

            List<Integer> withNull = new ArrayList<>(Arrays.asList(2, null, 1));
            withNull.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
            ck("Comparator.nullsFirst", withNull, Arrays.asList(null, 1, 2));

            // --- Iterator + remove ---
            List<Integer> itList = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
            Iterator<Integer> it = itList.iterator();
            while (it.hasNext()) {
                if (it.next() % 2 == 0) it.remove();
            }
            ck("Iterator.remove", itList, Arrays.asList(1, 3));

            // --- ListIterator (bidirectional + set) ---
            List<Integer> liList = new ArrayList<>(Arrays.asList(10, 20, 30));
            ListIterator<Integer> li = liList.listIterator();
            ck("ListIterator.first", li.next(), 10);
            li.set(99);
            ck("ListIterator.next2", li.next(), 20);
            ck("ListIterator.previous", li.previous(), 20);
            ck("ListIterator.afterSet", liList, Arrays.asList(99, 20, 30));

        } catch (Throwable t) {
            F++;
            System.out.println("[FAIL] threw " + t);
        }
        System.out.println(CLS + ": " + P + "/" + (P + F) + " passed");
        System.out.println(CLS + " RESULT: " + (F == 0 ? "PASS" : "FAIL"));
        System.exit(F == 0 ? 0 : 1);
    }
}
