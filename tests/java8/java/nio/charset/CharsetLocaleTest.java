package java8.java.nio.charset;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/*
 * CharsetLocaleTest -- on-VM half of the "Amiga-1251 NoClassDefFoundError" fix.
 *
 * The native fix (src/openjdk/amiga_charset.h, host-tested by
 * tools/test-amiga-charset.c) maps the AmigaOS diskfont charset name each locale
 * reports to a JDK charset name.  THIS test verifies the other half: that those
 * target names actually resolve on JamVM/OpenJDK 8 and correctly round-trip real
 * text for ten languages -- i.e. that the fix's output is not just loadable but
 * right.  It also asserts the running VM's own file.encoding/sun.jnu.encoding are
 * supported and no longer a raw "Amiga-*" name (the bug's signature).
 *
 * Self-verifying: prints [PASS]/[FAIL] + "CharsetLocaleTest RESULT: PASS|FAIL",
 * exits 0 on success.  Auto-discovered by tools/build-regression.sh (suite8).
 */
public class CharsetLocaleTest {
    static final String CLS = "CharsetLocaleTest";
    static int P, F;

    static void ck(String n, boolean ok) {
        if (ok) { P++; System.out.println("[PASS] " + n); }
        else    { F++; System.out.println("[FAIL] " + n); }
    }

    /* One locale: the AmigaOS CODESET name -> the JDK charset the fix yields,
     * plus a representative sample that must survive an encode/decode round-trip
     * in that charset. */
    static final class Case {
        final String lang, amigaCodeset, javaName, sample;
        Case(String lang, String amigaCodeset, String javaName, String sample) {
            this.lang = lang; this.amigaCodeset = amigaCodeset;
            this.javaName = javaName; this.sample = sample;
        }
    }

    static final Case[] CASES = {
        //    language            AmigaOS CODESET   JDK charset      sample text
        new Case("Russian",       "Amiga-1251",  "windows-1251", "Привет, мир!"), // Привет, мир!
        new Case("Italian",       "ISO-8859-1",  "ISO-8859-1",   "Però è città"),                              // Però è città
        new Case("German",        "ISO-8859-1",  "ISO-8859-1",   "Grüße schön"),                               // Grüße schön
        new Case("French",        "ISO-8859-15", "ISO-8859-15",  "Coût 5€ déjà"),                         // Coût 5€ déjà  (€ needs -15)
        new Case("Spanish",       "ISO-8859-15", "ISO-8859-15",  "Niño ¿cómo?"),                               // Niño ¿cómo?
        new Case("Polish",        "ISO-8859-2",  "ISO-8859-2",   "Zażółć gęślą jaźń"), // Zażółć gęślą jaźń
        new Case("Greek",         "ISO-8859-7",  "ISO-8859-7",   "Καλημέρα"),         // Καλημέρα
        new Case("Turkish",       "ISO-8859-9",  "ISO-8859-9",   "Günaydın şçğ"),                    // Günaydın şçğ
        new Case("English/UTF-8", "UTF-8",       "UTF-8",        "Hello — café"),                                   // Hello — café
        new Case("Czech",         "Amiga-1250",  "windows-1250", "Příliš žluťoučký"),      // Příliš žluťoučký
    };

    public static void main(String[] args) {
        try {
            System.out.println("== 10 languages: fix target charset loads + round-trips ==");
            for (Case c : CASES) {
                String tag = c.lang + " [" + c.amigaCodeset + " -> " + c.javaName + "]";
                // 1) the charset name the fix produces must be supported by this VM
                boolean supported = Charset.isSupported(c.javaName);
                ck(tag + " supported", supported);
                if (!supported) continue;
                // 2) it must encode AND decode this language's text losslessly
                try {
                    byte[] enc = c.sample.getBytes(c.javaName);
                    String dec = new String(enc, c.javaName);
                    ck(tag + " round-trip", dec.equals(c.sample));
                } catch (UnsupportedEncodingException e) {
                    ck(tag + " round-trip (" + e + ")", false);
                }
            }

            // 3) windows-1251 must be BYTE-EXACT with Amiga-1251 over the Cyrillic
            // block (Amiga-1251 was built from cp1251 chars 0xA8,0xB8,0xC0-0xFF;
            // see SDK Documentation/Localization/Charsets/Amiga-1251).  Decoding a
            // few key bytes via windows-1251 must give the exact Russian letters.
            System.out.println("== windows-1251 byte-exactness vs Amiga-1251 Cyrillic block ==");
            ckByte("0xC0->U+0410(A)",  0xC0, 'А'); // CYRILLIC CAPITAL A
            ckByte("0xCF->U+041F(Pe)", 0xCF, 'П'); // CYRILLIC CAPITAL PE
            ckByte("0xDF->U+042F(Ya)", 0xDF, 'Я'); // CYRILLIC CAPITAL YA
            ckByte("0xE0->U+0430(a)",  0xE0, 'а'); // CYRILLIC SMALL A
            ckByte("0xFF->U+044F(ya)", 0xFF, 'я'); // CYRILLIC SMALL YA
            ckByte("0xA8->U+0401(Io)", 0xA8, 'Ё'); // CYRILLIC CAPITAL IO
            ckByte("0xB8->U+0451(io)", 0xB8, 'ё'); // CYRILLIC SMALL IO

            // 4) end-to-end on THIS VM: whatever locale it booted in, the fix must
            // have left file.encoding/sun.jnu.encoding as a supported charset that
            // is NOT a raw "Amiga-*" name (the unfixed bug's exact signature -- an
            // unfixed Cyrillic boot would have crashed long before reaching here).
            System.out.println("== running VM default encodings ==");
            checkDefault("file.encoding");
            checkDefault("sun.jnu.encoding");
        }
        catch (Throwable t) { F++; System.out.println("[FAIL] threw " + t); }

        System.out.println(CLS + ": " + P + "/" + (P + F) + " passed");
        System.out.println(CLS + " RESULT: " + (F == 0 ? "PASS" : "FAIL"));
        System.exit(F == 0 ? 0 : 1);
    }

    static void ckByte(String n, int b, char expect) {
        try {
            String s = new String(new byte[]{ (byte) b }, "windows-1251");
            ck(n + " {got=" + (s.length() == 1 ? "U+" + Integer.toHexString(s.charAt(0)) : "?") + "}",
               s.length() == 1 && s.charAt(0) == expect);
        } catch (UnsupportedEncodingException e) {
            ck(n + " (" + e + ")", false);
        }
    }

    static void checkDefault(String prop) {
        String enc = System.getProperty(prop);
        ck(prop + " present {" + enc + "}", enc != null && enc.length() > 0);
        if (enc == null) return;
        ck(prop + " not raw Amiga-* {" + enc + "}", !enc.startsWith("Amiga"));
        ck(prop + " supported {" + enc + "}", safeSupported(enc));
    }

    static boolean safeSupported(String enc) {
        try { return Charset.isSupported(enc); }
        catch (Throwable t) { return false; }
    }
}
