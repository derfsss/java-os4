package java8.java.io;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class IoTest {
    static final String CLS = "IoTest";
    static int P, F;

    static void ck(String n, boolean ok) {
        if (ok) { P++; System.out.println("[PASS] " + n); }
        else { F++; System.out.println("[FAIL] " + n); }
    }

    static void ck(String n, Object got, Object exp) {
        ck(n + " {got=" + got + " exp=" + exp + "}", Objects.equals(got, exp));
    }

    // Serializable round-trip subject.
    static final class Point implements Serializable {
        private static final long serialVersionUID = 1L;
        final int x;
        final int y;
        final String label;
        transient int notSaved;
        Point(int x, int y, String label, int notSaved) {
            this.x = x; this.y = y; this.label = label; this.notSaved = notSaved;
        }
    }

    public static void main(String[] args) {
        File tmp = new File(CLS + ".tmp");
        try {
            // --- FileOutputStream / FileInputStream (raw bytes) ---
            byte[] payload = "Hello\nWorld".getBytes(StandardCharsets.UTF_8);
            try (FileOutputStream fos = new FileOutputStream(tmp)) {
                fos.write(payload);
            }
            byte[] readBack = new byte[payload.length];
            int n;
            try (FileInputStream fis = new FileInputStream(tmp)) {
                n = fis.read(readBack);
            }
            ck("FIS read count", n, payload.length);
            ck("FIS bytes match", new String(readBack, StandardCharsets.UTF_8), "Hello\nWorld");

            // --- File metadata on the temp file ---
            ck("File.exists", tmp.exists(), true);
            ck("File.isFile", tmp.isFile(), true);
            ck("File.length", tmp.length(), (long) payload.length);
            ck("File.getName", tmp.getName(), CLS + ".tmp");

            // --- FileWriter / FileReader (char stream) ---
            try (FileWriter fw = new FileWriter(tmp)) {
                fw.write("alpha line\nbeta line\ngamma line\n");
            }
            StringBuilder all = new StringBuilder();
            char[] cbuf = new char[8];
            try (FileReader fr = new FileReader(tmp)) {
                int r;
                while ((r = fr.read(cbuf)) != -1) all.append(cbuf, 0, r);
            }
            ck("FileReader content", all.toString(), "alpha line\nbeta line\ngamma line\n");

            // --- BufferedReader.readLine loop ---
            int lines = 0;
            String first = null;
            try (BufferedReader br = new BufferedReader(new FileReader(tmp))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (first == null) first = line;
                    lines++;
                }
            }
            ck("BufferedReader line count", lines, 3);
            ck("BufferedReader first line", first, "alpha line");

            // --- InputStreamReader / OutputStreamWriter with UTF-8 (round trip) ---
            String unicode = "café über naïve €";
            try (OutputStreamWriter osw =
                     new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
                osw.write(unicode);
            }
            StringBuilder usb = new StringBuilder();
            try (InputStreamReader isr =
                     new InputStreamReader(new FileInputStream(tmp), StandardCharsets.UTF_8)) {
                int r;
                while ((r = isr.read()) != -1) usb.append((char) r);
            }
            ck("UTF-8 ISR/OSW round trip", usb.toString(), unicode);

            // --- ByteArrayOutputStream / ByteArrayInputStream ---
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write('J');
            baos.write("amVM".getBytes(StandardCharsets.UTF_8));
            byte[] arr = baos.toByteArray();
            ck("BAOS size", baos.size(), 5);
            ck("BAOS toString", baos.toString("UTF-8"), "JamVM");
            ByteArrayInputStream bais = new ByteArrayInputStream(arr);
            ck("BAIS available", bais.available(), 5);
            ck("BAIS first byte", bais.read(), (int) 'J');

            // --- DataOutputStream / DataInputStream ---
            ByteArrayOutputStream dbaos = new ByteArrayOutputStream();
            try (DataOutputStream dos = new DataOutputStream(dbaos)) {
                dos.writeInt(0x0BADF00D);
                dos.writeUTF("däta");
                dos.writeDouble(3.141592653589793);
                dos.writeBoolean(true);
            }
            try (DataInputStream dis =
                     new DataInputStream(new ByteArrayInputStream(dbaos.toByteArray()))) {
                ck("DIS readInt", dis.readInt(), 0x0BADF00D);
                ck("DIS readUTF", dis.readUTF(), "däta");
                ck("DIS readDouble", dis.readDouble(), 3.141592653589793);
                ck("DIS readBoolean", dis.readBoolean(), true);
            }

            // --- PrintWriter to a StringWriter ---
            StringWriter sw = new StringWriter();
            try (PrintWriter pw = new PrintWriter(sw)) {
                pw.print("num=");
                pw.println(42);
                pw.printf((java.util.Locale) null == null ? java.util.Locale.US : java.util.Locale.US,
                          "pi=%.2f", 3.14159);
            }
            ck("PrintWriter+StringWriter", sw.toString(), "num=42" + System.lineSeparator() + "pi=3.14");

            // --- StringReader ---
            StringBuilder srb = new StringBuilder();
            try (StringReader sr = new StringReader("read-me")) {
                int r;
                while ((r = sr.read()) != -1) srb.append((char) r);
            }
            ck("StringReader content", srb.toString(), "read-me");

            // --- Serialization round trip over a byte array ---
            Point orig = new Point(7, -3, "origin", 999);
            ByteArrayOutputStream objBytes = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(objBytes)) {
                oos.writeObject(orig);
            }
            Point copy;
            try (ObjectInputStream ois =
                     new ObjectInputStream(new ByteArrayInputStream(objBytes.toByteArray()))) {
                copy = (Point) ois.readObject();
            }
            ck("Serial x", copy.x, 7);
            ck("Serial y", copy.y, -3);
            ck("Serial label", copy.label, "origin");
            ck("Serial transient not saved", copy.notSaved, 0);

        } catch (Throwable t) {
            F++; System.out.println("[FAIL] threw " + t);
        } finally {
            if (tmp.exists()) {
                boolean del = tmp.delete();
                ck("temp file deleted", del, true);
            }
        }
        System.out.println(CLS + ": " + P + "/" + (P + F) + " passed");
        System.out.println(CLS + " RESULT: " + (F == 0 ? "PASS" : "FAIL"));
        System.exit(F == 0 ? 0 : 1);
    }
}
