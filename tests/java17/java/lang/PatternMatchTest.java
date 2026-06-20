package java17.java.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PatternMatchTest {
    static final String CLS = "PatternMatchTest";
    static int P, F;

    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", java.util.Objects.equals(got, exp)); }

    // instanceof pattern: binding s usable directly
    static int describeLen(Object o){
        if (o instanceof String s) return s.length();
        return -1;
    }

    // pattern combined with && guard
    static boolean isPositiveInt(Object o){
        return o instanceof Integer i && i > 0;
    }

    // pattern binding scope in early-return (flow scoping)
    static String classify(Object o){
        if (!(o instanceof Number n)) return "not-a-number";
        // n is in scope here because the negated instanceof returned early
        return "number=" + n.intValue();
    }

    // dispatch by instanceof pattern over List<Object>
    static String dispatch(Object o){
        if (o instanceof String s) return "S:" + s.length();
        else if (o instanceof Integer i && i % 2 == 0) return "EVEN:" + i;
        else if (o instanceof Integer i) return "ODD:" + i;
        else if (o instanceof List<?> l) return "L:" + l.size();
        else return "OTHER";
    }

    public static void main(String[] args) {
        try {
            // --- instanceof pattern: s used directly ---
            ck("instanceof String binds s", describeLen("hello"), 5);
            ck("instanceof String non-match", describeLen(42), -1);

            Object o1 = "world";
            if (o1 instanceof String s) {
                ck("direct use binding length", s.length(), 5);
                ck("direct use binding upper", s.toUpperCase(), "WORLD");
            } else {
                ck("o1 should be String", false);
            }

            // --- pattern combined with && ---
            ck("positive int 7", isPositiveInt(7), true);
            ck("non-positive int -3", isPositiveInt(-3), false);
            ck("zero not positive", isPositiveInt(0), false);
            ck("string not positive int", isPositiveInt("x"), false);

            Object o2 = Integer.valueOf(10);
            if (o2 instanceof Integer i && i > 5) {
                ck("guarded binding value", i.intValue(), 10);
            } else {
                ck("o2 should match guard", false);
            }

            // --- early-return flow scoping ---
            ck("classify number", classify(99), "number=99");
            ck("classify non-number", classify("abc"), "not-a-number");
            ck("classify double truncates", classify(3.9), "number=3");

            // --- local var type inference ---
            var list = new ArrayList<String>();   // diamond via var
            list.add("a"); list.add("bb"); list.add("ccc");
            ck("var ArrayList size", list.size(), 3);

            var count = 0;                        // var int
            for (var elem : list) {               // var enhanced-for element
                count += elem.length();
            }
            ck("var enhanced-for sum lengths", count, 6);

            var greeting = "hi-" + list.get(0);   // var String
            ck("var String concat", greeting, "hi-a");

            var total = 0L;                       // var long
            for (var n = 1; n <= 5; n++) total += n;
            ck("var long accumulate", total, 15L);

            // --- dispatch over List<Object> by instanceof pattern ---
            List<Object> mixed = new ArrayList<>();
            mixed.add("hey");
            mixed.add(4);
            mixed.add(7);
            mixed.add(List.of(1, 2));
            mixed.add(3.14);

            var results = new ArrayList<String>();
            for (var item : mixed) {
                results.add(dispatch(item));
            }
            ck("dispatch String", results.get(0), "S:3");
            ck("dispatch even int", results.get(1), "EVEN:4");
            ck("dispatch odd int", results.get(2), "ODD:7");
            ck("dispatch list", results.get(3), "L:2");
            ck("dispatch other (double)", results.get(4), "OTHER");
            ck("dispatch count", results.size(), 5);

            // nested pattern + guard in single expression
            Object boxed = "abcd";
            boolean longStr = boxed instanceof String s && s.length() >= 4;
            ck("nested pattern length guard", longStr, true);

        } catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println(CLS+": "+P+"/"+(P+F)+" passed");
        System.out.println(CLS+" RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
