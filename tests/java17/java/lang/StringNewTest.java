package java17.java.lang;

import java.util.List;
import java.util.stream.Collectors;

public class StringNewTest {
    static final String CLS = "StringNewTest";
    static int P, F;

    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", java.util.Objects.equals(got, exp)); }

    public static void main(String[] args) {
        try {
            // strip family vs trim. U+2003 EM SPACE is unicode whitespace that trim() does NOT remove.
            String em = " "; // unicode space, > 0x20
            String s = em + "hi" + em;
            ck("strip removes unicode ws", s.strip(), "hi");
            ck("stripLeading", s.stripLeading(), "hi" + em);
            ck("stripTrailing", s.stripTrailing(), em + "hi");
            // trim only strips chars <= 0x20, so unicode em-space remains
            ck("trim keeps unicode ws", s.trim(), s);
            ck("trim removes ascii ws", "  hi  ".trim(), "hi");
            ck("strip ascii ws", "  hi  ".strip(), "hi");

            // isBlank
            ck("isBlank empty", "".isBlank(), true);
            ck("isBlank spaces+unicode", (" \t" + em).isBlank(), true);
            ck("isBlank false", " x ".isBlank(), false);

            // repeat
            ck("repeat 3", "ab".repeat(3), "ababab");
            ck("repeat 0", "ab".repeat(0), "");
            ck("repeat empty", "".repeat(5), "");

            // lines()
            String multi = "a\nb\r\nc\nd";
            ck("lines count", multi.lines().count(), 4L);
            List<String> ls = multi.lines().collect(Collectors.toList());
            ck("lines collect", ls, List.of("a", "b", "c", "d"));
            ck("lines trailing newline", "x\n".lines().count(), 1L);

            // chars() / codePoints()
            ck("chars count", "hello".chars().count(), 5L);
            // surrogate pair: U+1F600 -> 2 chars, 1 codepoint
            String emoji = "a😀b";
            ck("chars count surrogate", emoji.chars().count(), 4L);
            ck("codePoints count surrogate", emoji.codePoints().count(), 3L);
            ck("codePoint value", emoji.codePointAt(1), 0x1F600);

            // formatted (Java 15)
            ck("formatted", "%s=%d".formatted("x", 7), "x=7");
            ck("formatted pad", "[%5d]".formatted(42), "[   42]");

            // indent (Java 12) - adds n spaces and normalises line terminator to \n, appends \n
            ck("indent 2", "a\nb".indent(2), "  a\n  b\n");
            ck("indent 0 adds newline", "a".indent(0), "a\n");

            // String.valueOf parity
            ck("valueOf int", String.valueOf(123), "123");
            ck("valueOf boolean", String.valueOf(true), "true");
            ck("valueOf char", String.valueOf('Z'), "Z");
            ck("valueOf charArray", String.valueOf(new char[]{'a','b','c'}), "abc");
            ck("valueOf null obj", String.valueOf((Object) null), "null");
        }
        catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println(CLS+": "+P+"/"+(P+F)+" passed");
        System.out.println(CLS+" RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
