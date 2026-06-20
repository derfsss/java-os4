/*
 * HelloJava -- a tiny headless example shipped with Java-OS4.
 *
 * Shows that real Java 8 runs on AmigaOS 4: lambdas, streams and a few
 * library calls.  Build with a host JDK 8; run on the Amiga with:
 *
 *     java -cp examples/HelloJava.jar HelloJava
 *
 * GPLv2 (java-os4 project).
 */
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HelloJava {
    public static void main(String[] args) {
        System.out.println("Hello from Java on AmigaOS 4!");
        System.out.println("  java.version = " + System.getProperty("java.version"));
        System.out.println("  java.vm.name = " + System.getProperty("java.vm.name"));
        System.out.println("  os.name/arch = " + System.getProperty("os.name")
            + " / " + System.getProperty("os.arch"));

        List<String> amigas = Arrays.asList("A1222", "X5000", "Sam460", "Pegasos2");
        String joined = amigas.stream()
            .filter(s -> s.length() <= 5)
            .map(String::toUpperCase)
            .sorted()
            .collect(Collectors.joining(", "));
        System.out.println("  PowerPC Amigas (<=5 chars): " + joined);

        long sumOfSquares = amigas.stream()
            .mapToInt(String::length)
            .map(n -> n * n)
            .sum();
        System.out.println("  sum of name-length squares = " + sumOfSquares);

        System.out.println("Done.");
    }
}
