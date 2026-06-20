public class ExecTest {
    public static void main(String[] a) {
        try {
            Process p = Runtime.getRuntime().exec("list");
            System.out.println("exec unexpectedly worked: " + p);
        } catch (java.io.IOException e) {
            System.out.println("exec -> IOException (graceful): " + e.getMessage());
        } catch (Throwable t) {
            System.out.println("exec -> " + t.getClass().getName() + ": " + t.getMessage());
        }
        System.out.println("ExecTest done");
    }
}
