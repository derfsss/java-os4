package java8.java.lang.invoke;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;

public class LambdaTest {

    static int P, F;

    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", java.util.Objects.equals(got, exp)); }

    // custom functional interface with default + static methods
    @FunctionalInterface
    interface Greeter {
        String greet(String who);
        default String shout(String who){ return greet(who).toUpperCase(java.util.Locale.US); }
        static Greeter hello(){ return w -> "hello " + w; }
    }

    // helper class for method references
    static final class Box {
        final int v;
        Box(int v){ this.v = v; }
        int doubled(){ return v * 2; }
        static int square(int x){ return x * x; }
    }

    static String prefix(String s){ return "x:" + s; }

    public static void main(String[] args) {
        try {
            // --- Function + compose/andThen ---
            Function<Integer,Integer> plus3 = i -> i + 3;
            Function<Integer,Integer> times2 = i -> i * 2;
            ck("Function.apply", plus3.apply(4), 7);
            ck("Function.andThen", plus3.andThen(times2).apply(4), 14); // (4+3)*2
            ck("Function.compose", plus3.compose(times2).apply(4), 11); // (4*2)+3
            ck("Function.identity", Function.identity().apply("z"), "z");

            // --- BiFunction ---
            BiFunction<Integer,Integer,Integer> add = (a,b) -> a + b;
            ck("BiFunction.apply", add.apply(5, 6), 11);
            ck("BiFunction.andThen", add.andThen(times2).apply(5, 6), 22);

            // --- Predicate and/or/negate ---
            Predicate<Integer> even = i -> i % 2 == 0;
            Predicate<Integer> pos = i -> i > 0;
            ck("Predicate.test", even.test(4), Boolean.TRUE);
            ck("Predicate.and", even.and(pos).test(-2), Boolean.FALSE);
            ck("Predicate.or", even.or(pos).test(3), Boolean.TRUE);
            ck("Predicate.negate", even.negate().test(3), Boolean.TRUE);

            // --- Consumer ---
            List<String> sink = new ArrayList<>();
            Consumer<String> c1 = sink::add;
            Consumer<String> c2 = s -> sink.add(s + "!");
            c1.andThen(c2).accept("a");
            ck("Consumer.andThen", sink, Arrays.asList("a", "a!"));

            // --- Supplier ---
            Supplier<String> sup = () -> "supplied";
            ck("Supplier.get", sup.get(), "supplied");

            // --- UnaryOperator / BinaryOperator ---
            UnaryOperator<String> up = s -> s + s;
            ck("UnaryOperator", up.apply("ab"), "abab");
            BinaryOperator<Integer> max = BinaryOperator.maxBy(Comparator.naturalOrder());
            ck("BinaryOperator.maxBy", max.apply(3, 9), 9);

            // --- Int-specialized functions ---
            IntFunction<String> ifn = i -> "n" + i;
            ck("IntFunction", ifn.apply(7), "n7");
            ToIntFunction<String> len = String::length;
            ck("ToIntFunction", len.applyAsInt("hello"), 5);
            IntPredicate ip = i -> i > 10;
            ck("IntPredicate", ip.test(11), Boolean.TRUE);

            // --- Method references ---
            Function<String,String> staticRef = LambdaTest::prefix;            // static
            ck("methodref-static", staticRef.apply("q"), "x:q");

            String greeting = "Hello World";
            Supplier<String> boundRef = greeting::toUpperCase;                 // bound instance
            ck("methodref-bound", boundRef.get(), "HELLO WORLD");

            Function<String,Integer> unboundRef = String::length;             // unbound instance
            ck("methodref-unbound", unboundRef.apply("abcd"), 4);

            IntFunction<Box> ctorRef = Box::new;                              // constructor
            ck("methodref-ctor", ctorRef.apply(21).v, 21);

            // method ref to instance method (unbound) used as mapper
            Function<Box,Integer> dbl = Box::doubled;
            ck("methodref-unbound-instance", dbl.apply(new Box(8)), 16);
            ck("methodref-static-of-helper", Box.square(6), 36);

            // --- Comparator.comparing ---
            List<String> words = new ArrayList<>(Arrays.asList("ccc", "a", "bb"));
            words.sort(Comparator.comparing(String::length));
            ck("Comparator.comparing", words, Arrays.asList("a", "bb", "ccc"));
            words.sort(Comparator.comparing(String::length).reversed());
            ck("Comparator.reversed", words, Arrays.asList("ccc", "bb", "a"));

            // --- custom functional interface: lambda + default + static ---
            Greeter g = w -> "hi " + w;
            ck("custom-fi-lambda", g.greet("Sam"), "hi Sam");
            ck("custom-fi-default", g.shout("Sam"), "HI SAM");
            ck("custom-fi-static", Greeter.hello().greet("Sam"), "hello Sam");

            // --- capturing effectively-final locals ---
            int captured = 100;
            String suffix = "!!";
            Function<Integer,String> capFn = i -> (i + captured) + suffix;
            ck("capture-locals", capFn.apply(5), "105!!");

            // capture in a loop (each lambda captures its own effectively-final copy)
            List<Supplier<Integer>> suppliers = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                final int k = i;
                suppliers.add(() -> k * 10);
            }
            int sum = 0;
            for (Supplier<Integer> s : suppliers) sum += s.get();
            ck("capture-loop", sum, 30); // 0 + 10 + 20

        } catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println("LambdaTest: "+P+"/"+(P+F)+" passed");
        System.out.println("LambdaTest RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
