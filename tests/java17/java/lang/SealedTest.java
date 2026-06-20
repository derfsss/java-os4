package java17.java.lang;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Java 17 sealed types regression test.
 * Covers: sealed interface with permits, final / non-sealed implementations,
 * a sealed abstract class hierarchy, exhaustive instanceof dispatch,
 * Class.isSealed() + getPermittedSubclasses(), and a non-sealed subclass
 * being extended further.
 */
public class SealedTest {

    static final String CLS = "SealedTest";

    // ---- sealed interface hierarchy --------------------------------------
    sealed interface Shape permits Circle, Square, Rectangle {}

    static final class Circle implements Shape {
        final double r;
        Circle(double r) { this.r = r; }
    }

    static final class Square implements Shape {
        final double side;
        Square(double side) { this.side = side; }
    }

    // non-sealed: may be extended further outside the permits clause
    static non-sealed class Rectangle implements Shape {
        final double w, h;
        Rectangle(double w, double h) { this.w = w; this.h = h; }
    }

    // a further extension of the non-sealed leaf (legal, no permits needed)
    static final class Box extends Rectangle {
        Box(double s) { super(s, s); }
    }

    // ---- sealed abstract class hierarchy ---------------------------------
    sealed static abstract class Expr permits Num, Add {
        abstract int eval();
    }

    static final class Num extends Expr {
        final int v;
        Num(int v) { this.v = v; }
        int eval() { return v; }
    }

    static final class Add extends Expr {
        final Expr a, b;
        Add(Expr a, Expr b) { this.a = a; this.b = b; }
        int eval() { return a.eval() + b.eval(); }
    }

    // exhaustive dispatch via instanceof pattern matching (Java 16+).  Switch
    // *type patterns* (case Circle c ->) are Java 21, so we stay on if/else to
    // remain valid at --release 17.
    static double area(Shape s) {
        if (s instanceof Circle c)    return Math.PI * c.r * c.r;
        if (s instanceof Square sq)   return sq.side * sq.side;
        if (s instanceof Rectangle r) return r.w * r.h;
        return -1.0; // conceptually unreachable
    }

    static int P, F;
    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", java.util.Objects.equals(got, exp)); }

    static Set<String> simpleNames(Class<?>[] cs) {
        return Arrays.stream(cs).map(Class::getSimpleName).collect(Collectors.toCollection(HashSet::new));
    }

    public static void main(String[] args) {
        try {
            // ---- area via exhaustive switch expression ----
            ck("area(Square 3)", area(new Square(3)), 9.0);
            ck("area(Rectangle 2x5)", area(new Rectangle(2, 5)), 10.0);
            ck("area(Box 4)", area(new Box(4)), 16.0); // Box matches Rectangle arm
            double ca = area(new Circle(2));
            ck("area(Circle 2) ~= 4PI", Math.abs(ca - (Math.PI * 4)) < 1e-9);

            // ---- sealed abstract class hierarchy eval ----
            Expr e = new Add(new Num(7), new Add(new Num(10), new Num(3)));
            ck("Expr eval 7+(10+3)", e.eval(), 20);

            // ---- isSealed flags ----
            ck("Shape.isSealed", Shape.class.isSealed(), true);
            ck("Expr.isSealed", Expr.class.isSealed(), true);
            ck("Circle.isSealed (final)", Circle.class.isSealed(), false);
            ck("Rectangle.isSealed (non-sealed)", Rectangle.class.isSealed(), false);
            ck("Box.isSealed (plain final)", Box.class.isSealed(), false);

            // final leaves are final; non-sealed leaf is not
            ck("Circle is final", java.lang.reflect.Modifier.isFinal(Circle.class.getModifiers()), true);
            ck("Rectangle not final", java.lang.reflect.Modifier.isFinal(Rectangle.class.getModifiers()), false);

            // ---- getPermittedSubclasses ----
            Set<String> shapePerms = simpleNames(Shape.class.getPermittedSubclasses());
            ck("Shape permitted set", shapePerms, new HashSet<>(Arrays.asList("Circle", "Square", "Rectangle")));
            ck("Shape permitted count", Shape.class.getPermittedSubclasses().length, 3);

            Set<String> exprPerms = simpleNames(Expr.class.getPermittedSubclasses());
            ck("Expr permitted set", exprPerms, new HashSet<>(Arrays.asList("Num", "Add")));

            // non-sealed and final classes have null permitted subclasses
            ck("Circle permitted is null", Circle.class.getPermittedSubclasses(), null);
            ck("Rectangle permitted is null", Rectangle.class.getPermittedSubclasses(), null);

            // ---- relationships ----
            ck("Box instanceof Rectangle", (new Box(1)) instanceof Rectangle, true);
            ck("Box instanceof Shape", (new Box(1)) instanceof Shape, true);
            ck("Box superclass is Rectangle", Box.class.getSuperclass(), Rectangle.class);
            ck("Circle implements Shape", Shape.class.isAssignableFrom(Circle.class), true);
        }
        catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println(CLS+": "+P+"/"+(P+F)+" passed");
        System.out.println(CLS+" RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
