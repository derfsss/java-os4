import java.util.*;
import java.util.stream.*;
import java.io.*;
import java.lang.annotation.*;

public class Conf3 {
    static int pass=0, fail=0;
    static void ok(String n){ System.out.println("[PASS] "+n); pass++; }
    static void bad(String n, Throwable t){ System.out.println("[FAIL] "+n+": "+t); fail++; }
    static void check(String n, boolean c){ if(c) ok(n); else bad(n,new RuntimeException("assert false")); }

    enum Color { RED, GREEN, BLUE; }
    @Retention(RetentionPolicy.RUNTIME) @interface Tag { String value(); }
    @Tag("hi") static class Tagged {}
    static class Res implements AutoCloseable { static boolean closed=false; public void close(){ closed=true; } }
    static int sum(int... xs){ int s=0; for(int x:xs) s+=x; return s; }

    public static void main(String[] a) throws Exception {
        // Serialization round-trip
        try { HashMap<String,Integer> m=new HashMap<String,Integer>();
              for(int i=0;i<100;i++) m.put("k"+i,i);
              ByteArrayOutputStream bo=new ByteArrayOutputStream();
              ObjectOutputStream oo=new ObjectOutputStream(bo); oo.writeObject(m); oo.close();
              ObjectInputStream oi=new ObjectInputStream(new ByteArrayInputStream(bo.toByteArray()));
              @SuppressWarnings("unchecked") HashMap<String,Integer> m2=(HashMap<String,Integer>)oi.readObject(); oi.close();
              check("serialization", m2.size()==100 && m2.get("k50")==50); } catch(Throwable t){ bad("serialization",t); }

        // Enums
        try { Color c=Color.valueOf("GREEN"); String s="";
              switch(c){ case RED: s="r"; break; case GREEN: s="g"; break; default: s="b"; }
              check("enums", Color.values().length==3 && c.ordinal()==1 && s.equals("g")); } catch(Throwable t){ bad("enums",t); }

        // try-with-resources
        try { try(Res r=new Res()){ } check("try.resources", Res.closed); } catch(Throwable t){ bad("try.resources",t); }

        // Annotations + reflection
        try { Tag tg=Tagged.class.getAnnotation(Tag.class);
              check("annotations", tg!=null && tg.value().equals("hi")); } catch(Throwable t){ bad("annotations",t); }

        // Optional (Java 8)
        try { Optional<String> o=Optional.of("x").map(s->s+"y");
              check("optional", o.isPresent() && o.get().equals("xy") && !Optional.empty().isPresent()); } catch(Throwable t){ bad("optional",t); }

        // Comparator chaining (Java 8)
        try { List<String> l=new ArrayList<String>(Arrays.asList("bb","aa","a","b"));
              l.sort(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));
              check("comparator.chain", l.get(0).equals("a") && l.get(1).equals("b") && l.get(2).equals("aa")); } catch(Throwable t){ bad("comparator.chain",t); }

        // Varargs
        try { check("varargs", sum(1,2,3,4)==10 && sum()==0); } catch(Throwable t){ bad("varargs",t); }

        // Nested generics
        try { Map<String,List<Integer>> m=new HashMap<String,List<Integer>>();
              m.put("a", Arrays.asList(1,2,3));
              check("nested.generics", m.get("a").get(2)==3); } catch(Throwable t){ bad("nested.generics",t); }

        // String.format specifiers
        try { check("format.specifiers", String.format("%x",255).equals("ff")
                && String.format("%,d",1234567).equals("1,234,567")
                && String.format("%.2f",3.14159).equals("3.14")); } catch(Throwable t){ bad("format.specifiers",t); }

        // Collectors.groupingBy (stream)
        try { Map<Integer,List<String>> g=Stream.of("a","bb","cc","d").collect(Collectors.groupingBy(String::length));
              check("collectors.grouping", g.get(1).size()==2 && g.get(2).size()==2); } catch(Throwable t){ bad("collectors.grouping",t); }

        // System.arraycopy + Arrays.copyOf
        try { int[] src={1,2,3,4,5}; int[] dst=Arrays.copyOf(src,3);
              int[] d2=new int[5]; System.arraycopy(src,1,d2,0,4);
              check("arraycopy", dst.length==3 && dst[2]==3 && d2[3]==5); } catch(Throwable t){ bad("arraycopy",t); }

        // A small real algorithm: sieve of Eratosthenes
        try { boolean[] sieve=new boolean[1000]; int primes=0;
              for(int i=2;i<1000;i++){ if(!sieve[i]){ primes++; for(int j=2*i;j<1000;j+=i) sieve[j]=true; } }
              check("sieve", primes==168); } catch(Throwable t){ bad("sieve",t); }

        System.out.println("SUMMARY: "+pass+" passed, "+fail+" failed");
    }
}
