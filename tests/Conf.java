import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.lang.reflect.*;

public class Conf {
    static int pass = 0, fail = 0;
    static void ok(String n){ System.out.println("[PASS] "+n); pass++; }
    static void bad(String n, Throwable t){ System.out.println("[FAIL] "+n+": "+t); fail++; }
    static void check(String n, boolean c){ if(c) ok(n); else bad(n, new RuntimeException("assert false")); }

    public static void main(String[] a) throws Exception {
        try { check("string.ops", "Hello,World".split(",").length==2
                && "abc".toUpperCase().equals("ABC")
                && String.format("%05d",42).equals("00042")); } catch(Throwable t){ bad("string.ops",t); }

        try { check("math", Math.abs(Math.sqrt(16.0)-4.0)<1e-9
                && Math.abs(Math.pow(2,10)-1024.0)<1e-9
                && Math.max(3,7)==7); } catch(Throwable t){ bad("math",t); }

        try { int[] ar={5,3,1,4,2}; Arrays.sort(ar);
              check("arrays.sort", ar[0]==1 && ar[4]==5 && Arrays.binarySearch(ar,4)==3); } catch(Throwable t){ bad("arrays",t); }

        try { TreeMap<String,Integer> m=new TreeMap<String,Integer>();
              m.put("b",2); m.put("a",1); m.put("c",3);
              check("treemap", m.firstKey().equals("a") && m.get("c")==3); } catch(Throwable t){ bad("treemap",t); }

        try { HashSet<Integer> s=new HashSet<Integer>();
              for(int i=0;i<100;i++){ s.add(i); s.add(i); }
              check("hashset", s.size()==100 && s.contains(50)); } catch(Throwable t){ bad("hashset",t); }

        try { Pattern p=Pattern.compile("([0-9]+)-([0-9]+)"); Matcher mt=p.matcher("12-34");
              check("regex", mt.matches() && mt.group(1).equals("12") && mt.group(2).equals("34")); } catch(Throwable t){ bad("regex",t); }

        try { boolean caught=false; try{ throw new IllegalStateException("x"); }
              catch(IllegalStateException e){ caught=e.getMessage().equals("x"); }
              check("exceptions", caught); } catch(Throwable t){ bad("exceptions",t); }

        try { Class<?> c=Class.forName("java.lang.String");
              Method mm=c.getMethod("length"); int len=(Integer)mm.invoke("hello");
              check("reflection", len==5); } catch(Throwable t){ bad("reflection",t); }

        try { check("boxing.parse", Integer.parseInt("123")==123
                && Long.parseLong("9999999999")==9999999999L
                && Double.parseDouble("3.14")>3.1); } catch(Throwable t){ bad("boxing.parse",t); }

        try { final int[] counter={0};
              Thread th=new Thread(new Runnable(){ public void run(){ for(int i=0;i<1000;i++) counter[0]++; }});
              th.start(); th.join();
              check("threads", counter[0]==1000); } catch(Throwable t){ bad("threads",t); }

        try { StringBuilder sb=new StringBuilder();
              for(int i=0;i<500;i++) sb.append(i).append(',');
              check("stringbuilder", sb.length()>1000 && sb.toString().startsWith("0,1,2")); } catch(Throwable t){ bad("stringbuilder",t); }

        try { File f=new File("RAM:conf_io.txt");
              FileWriter w=new FileWriter(f); w.write("line1\nline2\n"); w.close();
              BufferedReader r=new BufferedReader(new FileReader(f));
              String l1=r.readLine(); r.close(); f.delete();
              check("file.io", "line1".equals(l1)); } catch(Throwable t){ bad("file.io",t); }

        try { check("collections.iter", new ArrayList<Integer>(Arrays.asList(1,2,3)).size()==3); } catch(Throwable t){ bad("collections.iter",t); }

        System.out.println("SUMMARY: "+pass+" passed, "+fail+" failed");
    }
}
