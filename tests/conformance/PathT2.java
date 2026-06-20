import java.io.*; import java.net.*; import java.util.jar.*;
public class PathT2 {
    public static void main(String[] a) throws Exception {
        File f = new File("hw.zip").getCanonicalFile();
        System.out.println("canon='"+f.getPath()+"' exists="+f.exists());
        URL u = sun.net.www.ParseUtil.fileToEncodedURL(f);
        System.out.println("encURL='"+u+"' urlpath='"+u.getPath()+"'");
        try { sun.misc.FileURLMapper m = new sun.misc.FileURLMapper(u);
            System.out.println("mapper.path='"+m.getPath()+"' exists="+m.exists());
        } catch(Throwable t){ System.out.println("mapper ERR: "+t); }
        try { JarFile jf = new JarFile(f.getPath());
            System.out.println("jarfile entries="+jf.size()+" hasHW="+(jf.getEntry("HelloWorld.class")!=null));
            jf.close();
        } catch(Throwable t){ System.out.println("jarfile ERR: "+t); }
        try { URLClassLoader cl = new URLClassLoader(new URL[]{u});
            Class<?> c = cl.loadClass("HelloWorld");
            System.out.println("urlcl loaded: "+c.getName());
        } catch(Throwable t){ System.out.println("urlcl ERR: "+t); }
    }
}
