package java8.java.math;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

public class BigNumberTest {
    static final String CLS = "BigNumberTest";
    static int P, F;

    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", Objects.equals(got, exp)); }

    static BigInteger factorial(int n){
        BigInteger r = BigInteger.ONE;
        for(int i=2;i<=n;i++) r = r.multiply(BigInteger.valueOf(i));
        return r;
    }

    public static void main(String[] args) {
        try {
            // ---- BigInteger arithmetic ----
            BigInteger a = BigInteger.valueOf(100);
            BigInteger b = BigInteger.valueOf(7);
            ck("BI.add", a.add(b), BigInteger.valueOf(107));
            ck("BI.subtract", a.subtract(b), BigInteger.valueOf(93));
            ck("BI.multiply", a.multiply(b), BigInteger.valueOf(700));
            ck("BI.divide", a.divide(b), BigInteger.valueOf(14));
            ck("BI.mod", a.mod(b), BigInteger.valueOf(2));
            ck("BI.pow", BigInteger.valueOf(2).pow(10), BigInteger.valueOf(1024));
            ck("BI.modPow", BigInteger.valueOf(4).modPow(BigInteger.valueOf(13), BigInteger.valueOf(497)), BigInteger.valueOf(445));
            ck("BI.gcd", BigInteger.valueOf(48).gcd(BigInteger.valueOf(36)), BigInteger.valueOf(12));
            ck("BI.abs", BigInteger.valueOf(-55).abs(), BigInteger.valueOf(55));
            ck("BI.negate", BigInteger.valueOf(9).negate(), BigInteger.valueOf(-9));

            // ---- BigInteger shifts and bit ops ----
            ck("BI.shiftLeft", BigInteger.valueOf(1).shiftLeft(16), BigInteger.valueOf(65536));
            ck("BI.shiftRight", BigInteger.valueOf(65536).shiftRight(8), BigInteger.valueOf(256));
            BigInteger x = BigInteger.valueOf(0b1100);
            BigInteger y = BigInteger.valueOf(0b1010);
            ck("BI.and", x.and(y), BigInteger.valueOf(0b1000));
            ck("BI.or", x.or(y), BigInteger.valueOf(0b1110));
            ck("BI.xor", x.xor(y), BigInteger.valueOf(0b0110));
            ck("BI.bitLength", BigInteger.valueOf(255).bitLength(), 8);

            // ---- BigInteger predicates / compare / toString ----
            ck("BI.isProbablePrime", BigInteger.valueOf(97).isProbablePrime(40), true);
            ck("BI.notPrime", BigInteger.valueOf(100).isProbablePrime(40), false);
            ck("BI.compareTo", a.compareTo(b), 1);
            ck("BI.toString(16)", BigInteger.valueOf(255).toString(16), "ff");
            ck("BI.toString(2)", BigInteger.valueOf(10).toString(2), "1010");

            // ---- BigInteger constants + factorial ----
            ck("BI.ZERO", BigInteger.ZERO, BigInteger.valueOf(0));
            ck("BI.ONE", BigInteger.ONE, BigInteger.valueOf(1));
            ck("BI.TEN", BigInteger.TEN, BigInteger.valueOf(10));
            ck("BI.factorial(30)", factorial(30), new BigInteger("265252859812191058636308480000000"));

            // ---- BigDecimal arithmetic ----
            BigDecimal d1 = BigDecimal.valueOf(10.5);
            BigDecimal d2 = BigDecimal.valueOf(2.5);
            ck("BD.add", d1.add(d2), new BigDecimal("13.0"));
            ck("BD.subtract", d1.subtract(d2), new BigDecimal("8.0"));
            ck("BD.multiply", d1.multiply(d2), new BigDecimal("26.25"));
            ck("BD.divide", new BigDecimal("10").divide(new BigDecimal("3"), 4, RoundingMode.HALF_UP), new BigDecimal("3.3333"));
            ck("BD.setScale", new BigDecimal("2.5").setScale(3), new BigDecimal("2.500"));
            ck("BD.scale", new BigDecimal("1.230").scale(), 3);
            ck("BD.precision", new BigDecimal("123.45").precision(), 5);

            // compareTo vs equals: same value, different scale
            ck("BD.compareTo eq-value", new BigDecimal("2.0").compareTo(new BigDecimal("2.00")), 0);
            ck("BD.equals scale-sensitive", new BigDecimal("2.0").equals(new BigDecimal("2.00")), false);

            ck("BD.stripTrailingZeros", new BigDecimal("1.2000").stripTrailingZeros(), new BigDecimal("1.2"));
            ck("BD.movePointLeft", new BigDecimal("123.4").movePointLeft(2), new BigDecimal("1.234"));

            // classic 0.1 + 0.2 exact in BigDecimal
            ck("BD.0.1+0.2", new BigDecimal("0.1").add(new BigDecimal("0.2")), new BigDecimal("0.3"));

            // MathContext rounding to given precision
            BigDecimal mc = new BigDecimal("1").divide(new BigDecimal("3"), new MathContext(5));
            ck("BD.MathContext(5)", mc, new BigDecimal("0.33333"));

            ck("BD.toBigInteger", new BigDecimal("123.99").toBigInteger(), BigInteger.valueOf(123));

            // ---- RoundingMode differences on 2.5 / -2.5 ----
            ck("RM.HALF_UP", new BigDecimal("2.5").setScale(0, RoundingMode.HALF_UP), new BigDecimal("3"));
            ck("RM.HALF_EVEN", new BigDecimal("2.5").setScale(0, RoundingMode.HALF_EVEN), new BigDecimal("2"));
            ck("RM.FLOOR", new BigDecimal("2.7").setScale(0, RoundingMode.FLOOR), new BigDecimal("2"));
            ck("RM.CEILING", new BigDecimal("2.1").setScale(0, RoundingMode.CEILING), new BigDecimal("3"));
            ck("RM.FLOOR neg", new BigDecimal("-2.1").setScale(0, RoundingMode.FLOOR), new BigDecimal("-3"));
            ck("RM.CEILING neg", new BigDecimal("-2.1").setScale(0, RoundingMode.CEILING), new BigDecimal("-2"));
        }
        catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println(CLS+": "+P+"/"+(P+F)+" passed");
        System.out.println(CLS+" RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
