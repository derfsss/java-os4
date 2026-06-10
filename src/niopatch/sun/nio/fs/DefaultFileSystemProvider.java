/*
 * AmigaOS bootclasspath-prepend patch (java-os4 project).
 * Replaces the stock sun.nio.fs.DefaultFileSystemProvider whose os.name switch
 * (Solaris/Linux/Mac/AIX) throws AssertionError("Platform not recognized") on
 * AmigaOS.  Always creates the (pure-unix) LinuxFileSystemProvider; combined
 * with -Dsun.nio.fs.chdirAllowed=true its construction needs no libnio natives.
 * GPLv2+Classpath-exception, derived from OpenJDK 8u77.
 */
package sun.nio.fs;

import java.nio.file.spi.FileSystemProvider;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class DefaultFileSystemProvider {
    private DefaultFileSystemProvider() { }

    @SuppressWarnings("unchecked")
    private static FileSystemProvider createProvider(String cn) {
        Class<FileSystemProvider> c;
        try {
            c = (Class<FileSystemProvider>)Class.forName(cn);
        } catch (ClassNotFoundException x) {
            throw new AssertionError(x);
        }
        try {
            return c.newInstance();
        } catch (IllegalAccessException | InstantiationException x) {
            throw new AssertionError(x);
        }
    }

    /**
     * Returns the default FileSystemProvider.  On AmigaOS the generic unix
     * provider (the Linux one from the Temurin linux rt.jar) is used.
     */
    public static FileSystemProvider create() {
        return createProvider("sun.nio.fs.LinuxFileSystemProvider");
    }
}
