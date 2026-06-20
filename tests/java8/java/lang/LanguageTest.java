package java8.java.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LanguageTest {
    static int P, F;
    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", java.util.Objects.equals(got, exp)); }

    // ---- enum with switch ----
    enum Color {
        RED("r"), GREEN("g"), BLUE("b");
        final String code;
        Color(String c){ this.code = c; }
        String code(){ return code; }
    }

    static String classify(Color c){
        switch(c){
            case RED:   return "warm";
            case GREEN: return "cool";
            case BLUE:  return "cool";
            default:    return "?";
        }
    }

    // ---- generic bounded method ----
    static <T extends Comparable<T>> T maxOf(List<? extends T> xs){
        T m = xs.get(0);
        for(T x : xs) if(x.compareTo(m) > 0) m = x;
        return m;
    }

    // ---- bounded wildcard consumer ----
    static double sum(List<? extends Number> xs){
        double s = 0;
        for(Number n : xs) s += n.doubleValue();
        return s;
    }

    // ---- varargs ----
    static int sumv(int... vs){
        int s = 0;
        for(int v : vs) s += v;
        return s;
    }

    @SafeVarargs
    static <T> int count(T... ts){ return ts.length; }

    // ---- custom AutoCloseable ----
    static class Resource implements AutoCloseable {
        final boolean[] closedFlag;
        Resource(boolean[] f){ closedFlag = f; }
        @Override public void close(){ closedFlag[0] = true; }
        int value(){ return 42; }
    }

    // ---- generic nested class ----
    static class Box<T> {
        private final T v;
        Box(T v){ this.v = v; }
        T get(){ return v; }
    }

    // ---- init order tracking ----
    static final List<String> initLog = new ArrayList<String>();
    static int staticCounter;
    static { staticCounter = 1; initLog.add("static"); }
    int instanceCounter;
    { instanceCounter = 7; initLog.add("instance"); }
    LanguageTest(){ initLog.add("ctor"); }

    // ---- interface for anonymous class ----
    interface Greeter { String greet(String who); }

    static String switchOnString(String s){
        switch(s){
            case "one":   return "1";
            case "two":   return "2";
            case "three": return "3";
            default:      return "?";
        }
    }

    public static void main(String[] args) {
        try {
            // enum: values / valueOf / ordinal
            ck("enum.values.length", Color.values().length, 3);
            ck("enum.valueOf", Color.valueOf("GREEN"), Color.GREEN);
            ck("enum.ordinal RED", Color.RED.ordinal(), 0);
            ck("enum.ordinal BLUE", Color.BLUE.ordinal(), 2);
            ck("enum.name", Color.BLUE.name(), "BLUE");
            ck("enum.field", Color.GREEN.code(), "g");
            ck("switch-on-enum RED", classify(Color.RED), "warm");
            ck("switch-on-enum GREEN", classify(Color.GREEN), "cool");

            // switch-on-String
            ck("switch-on-String two", switchOnString("two"), "2");
            ck("switch-on-String default", switchOnString("nope"), "?");

            // generics + bounded wildcards
            List<Integer> ints = new ArrayList<Integer>();
            ints.add(3); ints.add(9); ints.add(5);
            ck("generic maxOf", maxOf(ints), Integer.valueOf(9));
            List<Double> dbls = new ArrayList<Double>();
            dbls.add(1.5); dbls.add(2.5);
            ck("wildcard sum(Double)", sum(dbls), 4.0);
            ck("wildcard sum(Integer)", sum(ints), 17.0);
            Box<String> bx = new Box<String>("hi");
            ck("generic Box.get", bx.get(), "hi");

            // varargs
            ck("varargs sum", sumv(1, 2, 3, 4), 10);
            ck("varargs empty", sumv(), 0);
            ck("varargs count", count("a", "b", "c"), 3);

            // autoboxing / unboxing
            Integer boxed = 100;
            int unboxed = boxed;
            ck("autobox->unbox", unboxed, 100);
            List<Integer> li = new ArrayList<Integer>();
            li.add(5);                 // autobox
            int first = li.get(0);     // unbox
            ck("autobox in collection", first, 5);
            Integer a = 127, b = 127;  // cached identity
            ck("Integer cache ==", a == b, true);

            // try-with-resources: close ran
            boolean[] closed = { false };
            int rv;
            try (Resource r = new Resource(closed)) {
                rv = r.value();
            }
            ck("try-with-resources value", rv, 42);
            ck("try-with-resources closed", closed[0], true);

            // anonymous class capturing a local
            final String prefix = "Hello, ";
            Greeter g = new Greeter(){
                @Override public String greet(String who){ return prefix + who; }
            };
            ck("anonymous capture", g.greet("World"), "Hello, World");

            // local class capturing a variable
            final int base = 10;
            class Adder { int add(int x){ return base + x; } }
            ck("local class capture", new Adder().add(5), 15);

            // labeled break
            int found = -1;
            outer:
            for(int i = 0; i < 5; i++){
                for(int j = 0; j < 5; j++){
                    if(i * 5 + j == 13){ found = i * 10 + j; break outer; }
                }
            }
            ck("labeled break", found, 23);

            // labeled continue
            int sumNoDiag = 0;
            rows:
            for(int i = 0; i < 3; i++){
                for(int j = 0; j < 3; j++){
                    if(i == j) continue rows;
                    sumNoDiag += 1;
                }
            }
            // continue rows skips the rest of the inner loop at i==j, so each
            // row i contributes its below-diagonal cells j<i: 0 + 1 + 2 = 3
            ck("labeled continue", sumNoDiag, 3);

            // instanceof + cast
            Object o = "text";
            String cast = (o instanceof String) ? (String) o : null;
            ck("instanceof+cast", cast, "text");
            Object num = Integer.valueOf(8);
            ck("instanceof negative", num instanceof String, false);

            // ternary
            int t = (5 > 3) ? 1 : 0;
            ck("ternary", t, 1);

            // multidimensional array
            int[][] grid = new int[2][3];
            grid[1][2] = 99;
            ck("2d array elem", grid[1][2], 99);
            ck("2d array rows", grid.length, 2);
            ck("2d array cols", grid[0].length, 3);

            // jagged array
            int[][] jag = new int[3][];
            jag[0] = new int[]{1};
            jag[1] = new int[]{1, 2};
            jag[2] = new int[]{1, 2, 3};
            int jagTotal = 0;
            for(int[] row : jag) for(int x : row) jagTotal += x;
            ck("jagged length row2", jag[2].length, 3);
            ck("jagged sum", jagTotal, 10);

            // enhanced for over array of strings
            String[] words = { "a", "bb", "ccc" };
            int charTotal = 0;
            for(String w : words) charTotal += w.length();
            ck("enhanced-for charTotal", charTotal, 6);

            // static vs instance initializer order
            ck("static init counter", staticCounter, 1);
            LanguageTest inst = new LanguageTest();
            ck("instance init counter", inst.instanceCounter, 7);
            // static ran once at class load (before any instance);
            // each instance: instance-block then ctor
            ck("init order log", initLog.toString(), "[static, instance, ctor]");
        }
        catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println("LanguageTest: "+P+"/"+(P+F)+" passed");
        System.out.println("LanguageTest RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
