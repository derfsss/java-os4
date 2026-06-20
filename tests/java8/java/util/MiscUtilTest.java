package java8.java.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

public class MiscUtilTest {
    static int P, F;

    static void ck(String n, boolean ok) {
        if (ok) { P++; System.out.println("[PASS] " + n); }
        else { F++; System.out.println("[FAIL] " + n); }
    }

    static void ck(String n, Object got, Object exp) {
        ck(n + " {got=" + got + " exp=" + exp + "}", java.util.Objects.equals(got, exp));
    }

    enum Color { RED, GREEN, BLUE, WHITE, BLACK }

    public static void main(String[] args) {
        try {
            // --- UUID ---
            String us = "12345678-1234-4234-8234-123456789abc";
            UUID u = UUID.fromString(us);
            ck("UUID.toString round-trip", u.toString(), us);
            ck("UUID.version", u.version(), 4);
            ck("UUID.variant", u.variant(), 2);
            ck("UUID.equals", u, UUID.fromString(us));
            // randomUUID() draws from SecureRandom; a JRE without an entropy
            // native (e.g. the Amiga port) throws UnsatisfiedLinkError here.
            // Guard it so the gap is one contained failure, not an abort that
            // would mask every check below.
            try {
                UUID r1 = UUID.randomUUID();
                UUID r2 = UUID.randomUUID();
                ck("randomUUID non-null", r1 != null && r2 != null);
                ck("randomUUID distinct", !r1.equals(r2));
            } catch (Throwable t) {
                ck("randomUUID (SecureRandom native missing: " + t + ")", false);
            }

            // --- Random with fixed seed (reproducible) ---
            Random rnd = new Random(42);
            ck("Random(42) nextInt#1", rnd.nextInt(), -1170105035);
            ck("Random(42) nextInt#2", rnd.nextInt(), 234785527);
            ck("Random(42) nextLong", rnd.nextLong(), -5843495416241995736L);
            ck("Random(42) nextBoolean", rnd.nextBoolean(), false);
            // Two independent instances with same seed agree
            Random a = new Random(7), b = new Random(7);
            ck("Random same-seed nextInt match", a.nextInt(1000), b.nextInt(1000));
            ck("Random same-seed nextDouble match", a.nextDouble(), b.nextDouble());

            // --- StringTokenizer ---
            StringTokenizer st = new StringTokenizer("one,two,,three", ",");
            ck("StringTokenizer countTokens", st.countTokens(), 3);
            ck("ST nextToken#1", st.nextToken(), "one");
            ck("ST nextToken#2", st.nextToken(), "two");
            ck("ST nextToken#3 (delim change)", st.nextToken("e"), ",,thr");

            // --- BitSet ---
            BitSet bs = new BitSet();
            bs.set(1); bs.set(3); bs.set(5);
            ck("BitSet get(3)", bs.get(3), true);
            ck("BitSet get(2)", bs.get(2), false);
            ck("BitSet cardinality", bs.cardinality(), 3);
            ck("BitSet length", bs.length(), 6);
            ck("BitSet nextSetBit(2)", bs.nextSetBit(2), 3);
            bs.clear(3);
            ck("BitSet after clear cardinality", bs.cardinality(), 2);
            bs.flip(3);
            ck("BitSet after flip get(3)", bs.get(3), true);
            BitSet x = new BitSet(); x.set(1); x.set(2);
            BitSet y = new BitSet(); y.set(2); y.set(4);
            BitSet andSet = (BitSet) x.clone(); andSet.and(y);
            ck("BitSet and cardinality", andSet.cardinality(), 1);
            ck("BitSet and bit2", andSet.get(2), true);
            BitSet orSet = (BitSet) x.clone(); orSet.or(y);
            ck("BitSet or cardinality", orSet.cardinality(), 3);
            BitSet xorSet = (BitSet) x.clone(); xorSet.xor(y);
            ck("BitSet xor cardinality", xorSet.cardinality(), 2);

            // --- EnumSet / EnumMap ---
            EnumSet<Color> es = EnumSet.of(Color.RED, Color.BLUE);
            ck("EnumSet.of size", es.size(), 2);
            ck("EnumSet contains BLUE", es.contains(Color.BLUE), true);
            ck("EnumSet.allOf size", EnumSet.allOf(Color.class).size(), 5);
            ck("EnumSet.complementOf size", EnumSet.complementOf(es).size(), 3);
            EnumSet<Color> range = EnumSet.range(Color.GREEN, Color.WHITE);
            ck("EnumSet.range size", range.size(), 3);
            ck("EnumSet.range contains GREEN", range.contains(Color.GREEN), true);
            EnumMap<Color, Integer> em = new EnumMap<>(Color.class);
            em.put(Color.RED, 1); em.put(Color.GREEN, 2);
            ck("EnumMap get", em.get(Color.GREEN), 2);
            ck("EnumMap size", em.size(), 2);

            // --- Properties (store + load) ---
            Properties props = new Properties();
            props.setProperty("name", "JamVM");
            props.setProperty("ver", "8");
            ck("Properties getProperty", props.getProperty("name"), "JamVM");
            ck("Properties default", props.getProperty("missing", "dflt"), "dflt");
            ByteArrayOutputStream pos = new ByteArrayOutputStream();
            props.store(pos, "test");
            Properties loaded = new Properties();
            loaded.load(new ByteArrayInputStream(pos.toByteArray()));
            ck("Properties store/load name", loaded.getProperty("name"), "JamVM");
            ck("Properties store/load ver", loaded.getProperty("ver"), "8");

            // --- Base64 ---
            byte[] data = "Hello, AmigaOS 4!".getBytes(StandardCharsets.UTF_8);
            String enc = Base64.getEncoder().encodeToString(data);
            ck("Base64 encode", enc, "SGVsbG8sIEFtaWdhT1MgNCE=");
            byte[] dec = Base64.getDecoder().decode(enc);
            ck("Base64 round-trip", Arrays.equals(dec, data), true);
            byte[] bin = { (byte) 0xfb, (byte) 0xff, (byte) 0xbf };
            String urlEnc = Base64.getUrlEncoder().encodeToString(bin);
            ck("Base64 URL encode", urlEnc, "-_-_");
            ck("Base64 URL round-trip",
                    Arrays.equals(Base64.getUrlDecoder().decode(urlEnc), bin), true);

            // --- Scanner over a String ---
            Scanner sc = new Scanner("42 hello 7");
            ck("Scanner nextInt", sc.nextInt(), 42);
            ck("Scanner next", sc.next(), "hello");
            ck("Scanner hasNextInt", sc.hasNextInt(), true);
            ck("Scanner nextInt#2", sc.nextInt(), 7);
            ck("Scanner hasNext at end", sc.hasNext(), false);
            sc.close();

            // --- Collections constants ---
            ck("Collections.emptyList isEmpty", Collections.emptyList().isEmpty(), true);
            ck("Collections.emptyMap isEmpty", Collections.emptyMap().isEmpty(), true);
            List<String> sing0 = Collections.singletonList("z");
            ck("Collections.singletonList size", sing0.size(), 1);
            ck("Collections.singletonList get", sing0.get(0), "z");
            Map<String, Integer> singMap = Collections.singletonMap("k", 9);
            ck("Collections.singletonMap get", singMap.get("k"), 9);
            Set<String> singSet = Collections.singleton("s");
            ck("Collections.singleton size", singSet.size(), 1);
        }
        catch (Throwable t) { F++; System.out.println("[FAIL] threw " + t); }
        System.out.println("MiscUtilTest: " + P + "/" + (P + F) + " passed");
        System.out.println("MiscUtilTest RESULT: " + (F == 0 ? "PASS" : "FAIL"));
        System.exit(F == 0 ? 0 : 1);
    }
}
