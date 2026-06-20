package java8.java.lang;

public class MathNumberTest {
    static final String CLS = "MathNumberTest";
    static int P, F;
    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", java.util.Objects.equals(got, exp)); }

    public static void main(String[] args) {
        try {
            // ---- java.lang.Math basic ----
            ck("Math.abs(int)", Math.abs(-7), 7);
            ck("Math.abs(double)", Math.abs(-3.5), 3.5);
            ck("Math.max", Math.max(4, 9), 9);
            ck("Math.min", Math.min(4, 9), 4);
            ck("Math.pow", Math.pow(2.0, 10.0), 1024.0);
            ck("Math.sqrt", Math.sqrt(144.0), 12.0);
            ck("Math.cbrt", Math.cbrt(27.0), 3.0);
            ck("Math.floor", Math.floor(2.9), 2.0);
            ck("Math.ceil", Math.ceil(2.1), 3.0);
            ck("Math.round(double)", Math.round(2.5), 3L);
            ck("Math.round(2.4)", Math.round(2.4), 2L);
            ck("Math.log", Math.abs(Math.log(Math.E) - 1.0) < 1e-12);
            ck("Math.log10", Math.log10(1000.0), 3.0);
            ck("Math.exp", Math.abs(Math.exp(0.0) - 1.0) < 1e-12);
            ck("Math.hypot", Math.hypot(3.0, 4.0), 5.0);
            ck("Math.toRadians", Math.abs(Math.toRadians(180.0) - Math.PI) < 1e-12);
            ck("Math.floorDiv", Math.floorDiv(-7, 2), -4);
            ck("Math.floorMod", Math.floorMod(-7, 2), 1);

            // ---- overflow throwing ArithmeticException ----
            boolean addThrew = false;
            try { Math.addExact(Integer.MAX_VALUE, 1); }
            catch (ArithmeticException e){ addThrew = true; }
            ck("Math.addExact overflow throws", addThrew);

            boolean mulThrew = false;
            try { Math.multiplyExact(Integer.MAX_VALUE, 2); }
            catch (ArithmeticException e){ mulThrew = true; }
            ck("Math.multiplyExact overflow throws", mulThrew);

            ck("Math.addExact ok", Math.addExact(100, 23), 123);
            ck("Math.multiplyExact ok", Math.multiplyExact(6, 7), 42);

            // ---- Integer ----
            ck("Integer.parseInt radix", Integer.parseInt("ff", 16), 255);
            ck("Integer.toBinaryString", Integer.toBinaryString(10), "1010");
            ck("Integer.toHexString", Integer.toHexString(255), "ff");
            ck("Integer.bitCount", Integer.bitCount(255), 8);
            ck("Integer.highestOneBit", Integer.highestOneBit(100), 64);
            ck("Integer.reverse", Integer.reverse(1), Integer.MIN_VALUE);
            ck("Integer.MAX_VALUE wrap", Integer.MAX_VALUE + 1, Integer.MIN_VALUE);
            ck("Integer.MIN_VALUE wrap", Integer.MIN_VALUE - 1, Integer.MAX_VALUE);
            ck("Integer.compare", Integer.compare(3, 7), -1);

            // ---- Long ----
            ck("Long.parseLong radix", Long.parseLong("100", 16), 256L);
            ck("Long.toHexString", Long.toHexString(255L), "ff");
            ck("Long.bitCount", Long.bitCount(0xFFL), 8);
            ck("Long.numberOfTrailingZeros", Long.numberOfTrailingZeros(8L), 3);
            ck("Long.MAX_VALUE wrap", Long.MAX_VALUE + 1L, Long.MIN_VALUE);
            ck("Long.compare", Long.compare(7L, 3L), 1);

            // ---- Double ----
            ck("Double.parseDouble", Double.parseDouble("2.5"), 2.5);
            ck("Double.isNaN", Double.isNaN(0.0 / 0.0), true);
            ck("Double.isInfinite", Double.isInfinite(1.0 / 0.0), true);
            ck("Double.compare", Double.compare(1.0, 2.0), -1);
            ck("Double.MAX_VALUE positive", Double.MAX_VALUE > 0.0, true);

            // ---- Boolean ----
            ck("Boolean.parseBoolean true", Boolean.parseBoolean("TRUE"), true);
            ck("Boolean.parseBoolean false", Boolean.parseBoolean("nope"), false);

            // ---- Integer cache identity (-128..127) ----
            Integer a = Integer.valueOf(100);
            Integer b = Integer.valueOf(100);
            ck("Integer cache identity in range", a == b, true);
            Integer c = Integer.valueOf(1000);
            Integer d = Integer.valueOf(1000);
            ck("Integer no cache out of range", c == d, false);
        }
        catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println(CLS+": "+P+"/"+(P+F)+" passed");
        System.out.println(CLS+" RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
