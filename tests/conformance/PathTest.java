import java.io.*;
import java.net.*;
public class PathTest {
    public static void main(String[] a) throws Exception {
        File f = new File("hw.zip");
        System.out.println("orig='" + f.getPath() + "' exists=" + f.exists());
        File cf = f.getCanonicalFile();
        System.out.println("canon='" + cf.getPath() + "' exists=" + cf.exists());
        System.out.println("abs='" + f.getAbsolutePath() + "'");
        URI uri = cf.toURI();
        System.out.println("uri='" + uri + "'");
        URL url = uri.toURL();
        System.out.println("url='" + url + "' path='" + url.getPath() + "'");
        File back = new File(url.getPath());
        System.out.println("back='" + back.getPath() + "' exists=" + back.exists());
    }
}
