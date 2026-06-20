package java8.java.lang;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public class StringTest {

    static int P, F;
    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", java.util.Objects.equals(got, exp)); }

    public static void main(String[] args) {
        try {
            String s = "Hello, World";

            // concat
            ck("concat", "foo".concat("bar"), "foobar");

            // substring
            ck("substring(7)", s.substring(7), "World");
            ck("substring(0,5)", s.substring(0, 5), "Hello");

            // indexOf / lastIndexOf
            ck("indexOf('o')", s.indexOf('o'), 4);
            ck("lastIndexOf('o')", s.lastIndexOf('o'), 8);
            ck("indexOf(\"World\")", s.indexOf("World"), 7);
            ck("indexOf missing", s.indexOf("xyz"), -1);

            // replace
            ck("replace char", "banana".replace('a', 'o'), "bonono");
            ck("replace seq", "a-b-c".replace("-", "+"), "a+b+c");

            // split
            ck("split", Arrays.toString("a,b,c".split(",")), "[a, b, c]");
            ck("split limit", "a,b,c".split(",").length, 3);

            // trim
            ck("trim", "  hi  ".trim(), "hi");

            // case
            ck("toUpperCase", "abc".toUpperCase(Locale.US), "ABC");
            ck("toLowerCase", "ABC".toLowerCase(Locale.US), "abc");

            // startsWith / endsWith / contains
            ck("startsWith", s.startsWith("Hello"), true);
            ck("endsWith", s.endsWith("World"), true);
            ck("contains", s.contains(", W"), true);

            // charAt
            ck("charAt(0)", s.charAt(0), 'H');

            // toCharArray
            ck("toCharArray", Arrays.toString("abc".toCharArray()), "[a, b, c]");

            // getBytes + new String(byte[], UTF_8)
            byte[] bytes = "Jéllo".getBytes(StandardCharsets.UTF_8);
            ck("getBytes UTF-8 len", bytes.length, 6);
            ck("roundtrip UTF-8", new String(bytes, StandardCharsets.UTF_8), "Jéllo");

            // compareTo
            ck("compareTo equal", "abc".compareTo("abc"), 0);
            ck("compareTo less", "abc".compareTo("abd") < 0, true);

            // equalsIgnoreCase
            ck("equalsIgnoreCase", "Hello".equalsIgnoreCase("HELLO"), true);

            // intern identity
            String built = new StringBuilder("inter").append("ned").toString();
            ck("intern identity", built.intern() == "interned", true);

            // String.format
            ck("format", String.format(Locale.US, "%d-%s-%.2f", 7, "x", 3.5), "7-x-3.50");

            // String.join
            ck("join", String.join("-", "a", "b", "c"), "a-b-c");

            // valueOf
            ck("valueOf int", String.valueOf(42), "42");
            ck("valueOf boolean", String.valueOf(true), "true");
            ck("valueOf char[]", String.valueOf(new char[]{'h','i'}), "hi");

            // StringBuilder
            StringBuilder sb = new StringBuilder("abc");
            sb.append("def");
            sb.insert(0, "X");
            ck("sb append+insert", sb.toString(), "Xabcdef");
            sb.reverse();
            ck("sb reverse", sb.toString(), "fedcbaX");
            sb.delete(0, 3);
            ck("sb delete", sb.toString(), "cbaX");
            sb.replace(0, 1, "ZZ");
            ck("sb replace", sb.toString(), "ZZbaX");
            sb.setLength(3);
            ck("sb setLength", sb.toString(), "ZZb");

            // StringBuffer
            StringBuffer bf = new StringBuffer("123");
            bf.append("45").insert(0, "0");
            ck("buffer append+insert", bf.toString(), "012345");
            bf.reverse();
            ck("buffer reverse", bf.toString(), "543210");

            // Character
            ck("Character.isDigit", Character.isDigit('7'), true);
            ck("Character.isLetter", Character.isLetter('A'), true);
            ck("Character.isWhitespace", Character.isWhitespace(' '), true);
            ck("Character.toUpperCase", Character.toUpperCase('a'), 'A');
            ck("Character.getNumericValue", Character.getNumericValue('9'), 9);
        }
        catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println("StringTest: "+P+"/"+(P+F)+" passed");
        System.out.println("StringTest RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
