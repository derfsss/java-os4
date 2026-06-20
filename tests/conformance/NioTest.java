import java.nio.file.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.io.*;
import java.util.*;

public class NioTest {
    static int pass=0, fail=0;
    static void ok(String n){ System.out.println("[PASS] "+n); pass++; }
    static void bad(String n, Throwable t){ System.out.println("[FAIL] "+n+": "+t); fail++; }
    static void check(String n, boolean c){ if(c) ok(n); else bad(n,new RuntimeException("assert false")); }

    public static void main(String[] a) throws Exception {
        try { Path p = Paths.get("/RAM:nio_t.txt");
              Files.write(p, "nio-hello\nline2\n".getBytes(StandardCharsets.UTF_8));
              check("files.write+exists", Files.exists(p)); } catch(Throwable t){ bad("files.write+exists",t); }

        try { Path p = Paths.get("/RAM:nio_t.txt");
              byte[] b = Files.readAllBytes(p);
              check("files.readAllBytes", new String(b, StandardCharsets.UTF_8).startsWith("nio-hello")); } catch(Throwable t){ bad("files.readAllBytes",t); }

        try { Path p = Paths.get("/RAM:nio_t.txt");
              List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
              check("files.readAllLines", lines.size()==2 && lines.get(1).equals("line2")); } catch(Throwable t){ bad("files.readAllLines",t); }

        try { Path p = Paths.get("/RAM:nio_t.txt");
              check("files.size+attrs", Files.size(p) == 16 && !Files.isDirectory(p) && Files.isRegularFile(p)); } catch(Throwable t){ bad("files.size+attrs",t); }

        try { Path d = Paths.get("/RAM:niodir");
              Files.createDirectory(d);
              check("files.createDirectory", Files.isDirectory(d)); } catch(Throwable t){ bad("files.createDirectory",t); }

        try { Path s = Paths.get("/RAM:nio_t.txt"), c = Paths.get("/RAM:niodir/copy.txt");
              Files.copy(s, c);
              check("files.copy", Files.size(c) == 16); } catch(Throwable t){ bad("files.copy",t); }

        try { DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get("/RAM:niodir"));
              int n=0; for (Path q : ds) n++; ds.close();
              check("files.dirstream", n==1); } catch(Throwable t){ bad("files.dirstream",t); }

        try { Path c = Paths.get("/RAM:niodir/copy.txt");
              Files.delete(c); Files.delete(Paths.get("/RAM:niodir"));
              check("files.delete", !Files.exists(c)); } catch(Throwable t){ bad("files.delete",t); }

        try { RandomAccessFile raf = new RandomAccessFile("/RAM:nio_ch.bin","rw");
              FileChannel ch = raf.getChannel();
              ByteBuffer wb = ByteBuffer.wrap("channel-data".getBytes());
              ch.write(wb);
              ch.position(0);
              ByteBuffer rb = ByteBuffer.allocate(12);
              int r = ch.read(rb);
              ch.close(); raf.close(); new File("/RAM:nio_ch.bin").delete();
              check("filechannel.rw", r==12 && new String(rb.array()).equals("channel-data")); } catch(Throwable t){ bad("filechannel.rw",t); }

        try { Path p = Paths.get("/RAM:nio_t.txt"); Files.delete(p);
              check("cleanup", !Files.exists(p)); } catch(Throwable t){ bad("cleanup",t); }

        System.out.println("SUMMARY: "+pass+" passed, "+fail+" failed");
    }
}
