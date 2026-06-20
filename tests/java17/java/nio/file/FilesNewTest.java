package java17.java.nio.file;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public class FilesNewTest {

    static final String CLS = "FilesNewTest";
    static int P, F;

    static void ck(String n, boolean ok){ if(ok){P++; System.out.println("[PASS] "+n);} else {F++; System.out.println("[FAIL] "+n);} }
    static void ck(String n, Object got, Object exp){ ck(n+" {got="+got+" exp="+exp+"}", java.util.Objects.equals(got, exp)); }

    public static void main(String[] args) {
        Path a = Path.of(CLS + ".tmp");
        Path b = Path.of(CLS + "_b.tmp");
        Path c = Path.of(CLS + "_c.tmp");
        try {
            // --- Files.writeString / readString round-trip (Java 11) ---
            String text = "Hello AmigaOS4\nLine twoé";
            Path w = Files.writeString(a, text, StandardCharsets.UTF_8);
            ck("writeString returns same path", w, a);
            ck("file exists after writeString", Files.exists(a), true);
            String read = Files.readString(a, StandardCharsets.UTF_8);
            ck("readString round-trip", read, text);

            // default-charset overloads (UTF-8 per spec)
            Files.writeString(c, "plain ascii");
            ck("readString default charset", Files.readString(c), "plain ascii");

            // --- Files.writeString APPEND (Java 11) ---
            Files.writeString(a, "\nappended", StandardCharsets.UTF_8,
                    StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            String appended = Files.readString(a, StandardCharsets.UTF_8);
            ck("writeString APPEND", appended, text + "\nappended");

            // --- Files.mismatch (Java 12) ---
            // b identical to a's appended content -> -1
            Files.writeString(b, appended, StandardCharsets.UTF_8);
            ck("mismatch identical files == -1", Files.mismatch(a, b), -1L);

            // make b differ at a known byte index
            byte[] aBytes = Files.readAllBytes(a);
            byte[] diff = aBytes.clone();
            int idx = 6; // within "Hello AmigaOS4"
            diff[idx] = (byte) (diff[idx] ^ 0xFF);
            Files.write(b, diff);
            ck("mismatch first differing index", Files.mismatch(a, b), (long) idx);

            // mismatch where one file is a prefix of the other -> length of shorter
            byte[] prefix = Arrays.copyOf(aBytes, 4);
            Files.write(b, prefix);
            ck("mismatch prefix returns shorter length", Files.mismatch(a, b), 4L);

            // --- InputStream.readAllBytes (Java 9) ---
            byte[] src = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
            try (InputStream in = new ByteArrayInputStream(src)) {
                byte[] all = in.readAllBytes();
                ck("readAllBytes length", all.length, src.length);
                ck("readAllBytes content equal", Arrays.equals(all, src), true);
                ck("readAllBytes drains stream", in.read(), -1);
            }

            // --- InputStream.readNBytes (Java 9, int len overload) ---
            try (InputStream in = new ByteArrayInputStream(src)) {
                byte[] first = in.readNBytes(5);
                ck("readNBytes count", first.length, 5);
                ck("readNBytes content", new String(first, StandardCharsets.US_ASCII), "01234");
                // request more than remaining: returns only remaining
                byte[] rest = in.readNBytes(100);
                ck("readNBytes remaining count", rest.length, src.length - 5);
                ck("readNBytes remaining content",
                        new String(rest, StandardCharsets.US_ASCII), "56789ABCDEF");
            }

            // readNBytes(byte[],off,len) (Java 9)
            try (InputStream in = new ByteArrayInputStream(src)) {
                byte[] buf = new byte[10];
                int n = in.readNBytes(buf, 2, 6);
                ck("readNBytes(buf,off,len) count", n, 6);
                ck("readNBytes(buf,off,len) placed bytes",
                        new String(buf, 2, 6, StandardCharsets.US_ASCII), "012345");
            }

            // --- InputStream.transferTo (Java 9) ByteArrayInputStream -> ByteArrayOutputStream ---
            try (InputStream in = new ByteArrayInputStream(src);
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                long moved = in.transferTo(bos);
                ck("transferTo bytes moved", moved, (long) src.length);
                ck("transferTo content equal", Arrays.equals(bos.toByteArray(), src), true);
            }

            // --- OutputStream.nullOutputStream (Java 11) ---
            try (OutputStream nul = OutputStream.nullOutputStream()) {
                nul.write(src);              // must not throw, discards
                nul.write(42);
                nul.flush();
                ck("nullOutputStream discards writes", true, true);
            }
            // writing to a closed nullOutputStream must throw IOException
            OutputStream closed = OutputStream.nullOutputStream();
            closed.close();
            boolean threw = false;
            try { closed.write(1); } catch (java.io.IOException e) { threw = true; }
            ck("nullOutputStream write-after-close throws", threw, true);

        }
        catch (Throwable t){ F++; System.out.println("[FAIL] threw "+t); }
        finally {
            try { Files.deleteIfExists(a); } catch (Exception ignore) {}
            try { Files.deleteIfExists(b); } catch (Exception ignore) {}
            try { Files.deleteIfExists(c); } catch (Exception ignore) {}
        }
        System.out.println(CLS+": "+P+"/"+(P+F)+" passed");
        System.out.println(CLS+" RESULT: "+(F==0?"PASS":"FAIL"));
        System.exit(F==0?0:1);
    }
}
