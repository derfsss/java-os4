package java17.java.lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordsTest {

    static int P, F;
    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", java.util.Objects.equals(got, exp)); }

    // basic record with a compact constructor that validates
    record Point(int x, int y) {
        Point {
            if (x < 0 || y < 0) throw new IllegalArgumentException("negative");
        }
        // explicit extra static factory + static field
        static final Point ORIGIN = new Point(0, 0);
        static Point of(int x, int y) { return new Point(x, y); }
    }

    interface Named {
        String name();
        default String greeting() { return "Hi " + name(); }
    }

    // record implementing an interface with a default method
    record Person(String name, int age) implements Named { }

    // generic record
    record Pair<A, B>(A first, B second) {
        Pair<B, A> swap() { return new Pair<>(second, first); }
    }

    public static void main(String[] args) {
        try {
            Point p = new Point(1, 2);

            // auto accessors
            ck("accessor x", p.x(), 1);
            ck("accessor y", p.y(), 2);

            // auto toString exact form
            ck("toString form", p.toString(), "Point[x=1, y=2]");

            // auto equals / hashCode contract
            Point p2 = new Point(1, 2);
            Point p3 = new Point(3, 4);
            ck("equals same value", p.equals(p2), true);
            ck("equals diff value", p.equals(p3), false);
            ck("equals null", p.equals(null), false);
            ck("equals other type", p.equals("Point[x=1, y=2]"), false);
            ck("hashCode contract", p.hashCode() == p2.hashCode(), true);
            ck("reflexive equals", p.equals(p), true);

            // compact constructor validation throws
            boolean threw = false;
            try { new Point(-1, 5); } catch (IllegalArgumentException e) { threw = true; }
            ck("compact ctor validates", threw, true);

            // explicit static factory + static field
            ck("static factory of()", Point.of(5, 6), new Point(5, 6));
            ck("static field ORIGIN", Point.ORIGIN, new Point(0, 0));

            // record class reflection-ish properties
            ck("is record class", Point.class.isRecord(), true);
            ck("record components count", Point.class.getRecordComponents().length, 2);
            ck("first component name", Point.class.getRecordComponents()[0].getName(), "x");

            // record implementing interface with default method
            Person per = new Person("Ada", 36);
            ck("person name accessor", per.name(), "Ada");
            ck("person age accessor", per.age(), 36);
            ck("interface default method", per.greeting(), "Hi Ada");
            ck("person is Named", per instanceof Named, true);
            ck("person toString", per.toString(), "Person[name=Ada, age=36]");

            // generic record
            Pair<String, Integer> pair = new Pair<>("k", 7);
            ck("generic first", pair.first(), "k");
            ck("generic second", pair.second(), 7);
            Pair<Integer, String> swapped = pair.swap();
            ck("generic swap first", swapped.first(), 7);
            ck("generic swap second", swapped.second(), "k");
            ck("generic toString", pair.toString(), "Pair[first=k, second=7]");

            // nested / local record
            record Box(String label, int n) {
                int doubled() { return n * 2; }
            }
            Box b = new Box("L", 21);
            ck("local record accessor", b.label(), "L");
            ck("local record method", b.doubled(), 42);
            ck("local record toString", b.toString(), "Box[label=L, n=21]");
            ck("local record is record", b.getClass().isRecord(), true);

            // records in a List
            List<Point> list = new ArrayList<>();
            list.add(new Point(1, 2));
            list.add(new Point(3, 4));
            ck("list contains by value", list.contains(new Point(1, 2)), true);
            ck("list indexOf by value", list.indexOf(new Point(3, 4)), 1);

            // records as Map keys (dedup by value)
            Map<Point, String> map = new HashMap<>();
            map.put(new Point(1, 2), "a");
            map.put(new Point(1, 2), "b");   // same value key -> overwrite
            map.put(new Point(9, 9), "c");
            ck("map dedup by value size", map.size(), 2);
            ck("map get by equal key", map.get(new Point(1, 2)), "b");
            ck("map get other key", map.get(new Point(9, 9)), "c");

        }
        catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println("RecordsTest: "+P+"/"+(P+F)+" passed");
        System.out.println("RecordsTest RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
