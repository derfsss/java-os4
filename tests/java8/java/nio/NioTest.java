package java8.java.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class NioTest {
    static final String CLS = "NioTest";
    static int P, F;

    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", java.util.Objects.equals(got, exp)); }

    public static void main(String[] args) {
        try {
            // ---- ByteBuffer allocate / position / limit / remaining ----
            ByteBuffer bb = ByteBuffer.allocate(16);
            ck("bb.capacity", bb.capacity(), 16);
            ck("bb.position.init", bb.position(), 0);
            ck("bb.limit.init", bb.limit(), 16);
            ck("bb.remaining.init", bb.remaining(), 16);

            // ---- put bytes + int + double, then flip and read back ----
            bb.put((byte) 0x7F);
            bb.putInt(0x01020304);
            bb.putDouble(3.5d);
            int posAfterPuts = bb.position();
            ck("bb.posAfterPuts", posAfterPuts, 1 + 4 + 8); // 13
            bb.flip();
            ck("bb.limitAfterFlip", bb.limit(), 13);
            ck("bb.posAfterFlip", bb.position(), 0);
            ck("bb.getByte", bb.get(), (byte) 0x7F);
            ck("bb.getInt", bb.getInt(), 0x01020304);
            ck("bb.getDouble", bb.getDouble(), 3.5d);
            ck("bb.remainingAtEnd", bb.remaining(), 0);

            // ---- byte order BIG vs LITTLE endian ----
            ByteBuffer big = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN);
            big.putInt(0x01020304).flip();
            ck("big.order", big.order(), ByteOrder.BIG_ENDIAN);
            ck("big.byte0", big.get(0), (byte) 0x01);
            ck("big.byte3", big.get(3), (byte) 0x04);

            ByteBuffer little = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            little.putInt(0x01020304).flip();
            ck("little.order", little.order(), ByteOrder.LITTLE_ENDIAN);
            ck("little.byte0", little.get(0), (byte) 0x04);
            ck("little.byte3", little.get(3), (byte) 0x01);

            // ---- asIntBuffer view ----
            ByteBuffer ib = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN);
            IntBuffer iv = ib.asIntBuffer();
            iv.put(0x0A0B0C0D).put(0x11223344);
            ck("asIntBuffer.cap", iv.capacity(), 2);
            ck("ib.getInt0", ib.getInt(0), 0x0A0B0C0D);
            ck("ib.getInt4", ib.getInt(4), 0x11223344);

            // ---- wrap + array ----
            byte[] backing = { 10, 20, 30, 40 };
            ByteBuffer wrapped = ByteBuffer.wrap(backing);
            ck("wrap.hasArray", wrapped.hasArray(), true);
            ck("wrap.get2", wrapped.get(2), (byte) 30);
            ck("wrap.array.same", wrapped.array() == backing, true);
            ck("wrap.arrayEquals", Arrays.equals(wrapped.array(), backing), true);

            // ---- CharBuffer ----
            CharBuffer cb = CharBuffer.allocate(5);
            cb.put('h').put('e').put('l').put('l').put('o');
            cb.flip();
            ck("cb.toString", cb.toString(), "hello");
            CharBuffer cw = CharBuffer.wrap("world");
            ck("cw.length", cw.length(), 5);
            ck("cw.charAt1", String.valueOf(cw.charAt(1)), "o");

            // ---- StandardCharsets UTF-8 round trip ----
            String text = "NioTest-éñü"; // accented chars
            byte[] enc = text.getBytes(StandardCharsets.UTF_8);
            String dec = new String(enc, StandardCharsets.UTF_8);
            ck("utf8.roundtrip", dec, text);
            ByteBuffer encBuf = StandardCharsets.UTF_8.encode("abc");
            ck("utf8.encode.remaining", encBuf.remaining(), 3);
            CharBuffer decBuf = StandardCharsets.UTF_8.decode(ByteBuffer.wrap("abc".getBytes(StandardCharsets.UTF_8)));
            ck("utf8.decode", decBuf.toString(), "abc");

            // ---- Files: write(byte[]) / readAllBytes / exists / size / delete ----
            Path p = Paths.get("NioTest.tmp");
            Path p2 = Paths.get("NioTest2.tmp");
            try {
                byte[] data = "hello-nio-bytes".getBytes(StandardCharsets.UTF_8);
                Files.write(p, data);
                ck("files.exists", Files.exists(p), true);
                ck("files.size", Files.size(p), (long) data.length);
                byte[] readBack = Files.readAllBytes(p);
                ck("files.readAllBytes", Arrays.equals(readBack, data), true);

                // ---- write(lines) / readAllLines ----
                List<String> lines = Arrays.asList("line1", "line2", "line3");
                Files.write(p, lines, StandardCharsets.UTF_8);
                List<String> got = Files.readAllLines(p, StandardCharsets.UTF_8);
                ck("files.readAllLines", got, lines);

                // ---- copy to a 2nd relative file ----
                Files.copy(p, p2);
                ck("files.copy.exists", Files.exists(p2), true);
                ck("files.copy.sameSize", Files.size(p2), Files.size(p));
                List<String> got2 = Files.readAllLines(p2, StandardCharsets.UTF_8);
                ck("files.copy.contents", got2, lines);
            } finally {
                boolean d1 = Files.deleteIfExists(p);
                boolean d2 = Files.deleteIfExists(p2);
                ck("files.deleteIfExists.p", d1, true);
                ck("files.deleteIfExists.p2", d2, true);
                ck("files.deleted.p", Files.exists(p), false);
                ck("files.deleted.p2", Files.exists(p2), false);
            }
        }
        catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        System.out.println(CLS+": "+P+"/"+(P+F)+" passed");
        System.out.println(CLS+" RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
