import java.util.*;
import java.util.stream.*;
import java.util.zip.*;
import java.text.*;
import java.nio.*;
import java.math.*;
import java.io.*;

public class Conf2 {
    static int pass=0, fail=0;
    static void ok(String n){ System.out.println("[PASS] "+n); pass++; }
    static void bad(String n, Throwable t){ System.out.println("[FAIL] "+n+": "+t); fail++; }
    static void check(String n, boolean c){ if(c) ok(n); else bad(n,new RuntimeException("assert false")); }

    interface Op { int apply(int a, int b); }

    public static void main(String[] a) throws Exception {
        // Java 8 lambda -> invokedynamic / MethodHandle
        try { Op add=(x,y)->x+y; Runnable r=()->{}; r.run();
              check("lambda", add.apply(3,4)==7); } catch(Throwable t){ bad("lambda",t); }

        // Stream API (uses lambdas heavily)
        try { int s=Stream.of(1,2,3,4,5).filter(x->x%2==0).mapToInt(x->x*x).sum();
              check("stream", s==20); } catch(Throwable t){ bad("stream",t); }

        // method reference + collectors
        try { List<String> l=Arrays.asList("bb","a","ccc");
              String j=l.stream().sorted(Comparator.comparingInt(String::length)).collect(Collectors.joining(","));
              check("stream.collect", j.equals("a,bb,ccc")); } catch(Throwable t){ bad("stream.collect",t); }

        // charset encode/decode
        try { byte[] b="héllo".getBytes("UTF-8"); String s=new String(b,"UTF-8");
              check("charset.utf8", s.equals("héllo") && b.length==6); } catch(Throwable t){ bad("charset.utf8",t); }

        // BigInteger / BigDecimal
        try { BigInteger bi=BigInteger.valueOf(2).pow(64);
              BigDecimal bd=new BigDecimal("1.5").multiply(new BigDecimal("2"));
              check("bignum", bi.toString().equals("18446744073709551616") && bd.compareTo(new BigDecimal("3.0"))==0); } catch(Throwable t){ bad("bignum",t); }

        // NIO ByteBuffer
        try { ByteBuffer bb=ByteBuffer.allocate(16); bb.putInt(0x11223344); bb.putLong(42L); bb.flip();
              check("nio.bytebuffer", bb.getInt()==0x11223344 && bb.getLong()==42L); } catch(Throwable t){ bad("nio.bytebuffer",t); }

        // DecimalFormat (java.text)
        try { DecimalFormat df=new DecimalFormat("#,##0.00");
              check("text.decimalformat", df.format(1234.5).equals("1,234.50")); } catch(Throwable t){ bad("text.decimalformat",t); }

        // SimpleDateFormat + Date (timezone-sensitive)
        try { SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
              sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
              String d=sdf.format(new Date(0L));
              check("text.dateformat", d.equals("1970-01-01")); } catch(Throwable t){ bad("text.dateformat",t); }

        // Calendar
        try { Calendar c=Calendar.getInstance(TimeZone.getTimeZone("UTC"));
              c.setTimeInMillis(0L);
              check("calendar", c.get(Calendar.YEAR)==1970 && c.get(Calendar.MONTH)==0); } catch(Throwable t){ bad("calendar",t); }

        // java.util.zip ZipFile (exercises libzip + getEntry patch)
        try { File zf=new File("RAM:conf_z.zip");
              ZipOutputStream zos=new ZipOutputStream(new FileOutputStream(zf));
              zos.putNextEntry(new ZipEntry("hello.txt")); zos.write("hi".getBytes()); zos.closeEntry(); zos.close();
              ZipFile z=new ZipFile(zf); ZipEntry e=z.getEntry("hello.txt");
              InputStream is=z.getInputStream(e); int n=is.read(); is.close(); z.close(); zf.delete();
              check("zip.readwrite", e!=null && n=='h'); } catch(Throwable t){ bad("zip.readwrite",t); }

        // RandomAccessFile
        try { File rf=new File("RAM:conf_raf.bin");
              RandomAccessFile raf=new RandomAccessFile(rf,"rw");
              raf.writeInt(0xCAFE); raf.seek(0); int v=raf.readInt(); raf.close(); rf.delete();
              check("randomaccessfile", v==0xCAFE); } catch(Throwable t){ bad("randomaccessfile",t); }

        // Directory listing
        try { File d=new File("RAM:"); String[] names=d.list();
              check("dir.list", names!=null && names.length>=0); } catch(Throwable t){ bad("dir.list",t); }

        System.out.println("SUMMARY: "+pass+" passed, "+fail+" failed");
    }
}
