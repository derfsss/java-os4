#!/bin/sh
# Build the Java-OS4 test suite + bundled examples.
#
#   build/testsuite.zip    VmSuite (broad VM coverage) + KeyBindTest +
#                          CloseTest + V9Bomb (class-version-gate fixture)
#   build/examples/*.jar   HelloJava (headless) + SwingDemo (Swing)
#
# Compiles against the Temurin 8 rt.jar, like the other Java build steps.
# Run inside the javaos4-build image:
#   docker run --rm -v "<proj>:/work" -w /work javaos4-build:latest \
#       sh /work/tools/build-tests.sh
set -e
JDK8=/opt/jdk8
RT="$JDK8/jre/lib/rt.jar"
JAVAC="$JDK8/bin/javac"
JAR="$JDK8/bin/jar"
B=/work/build
TC="$B/tests-out"
EX="$B/examples"
rm -rf "$TC"; mkdir -p "$TC/classes" "$EX"

echo "=== compiling test suite ==="
"$JAVAC" -source 8 -target 8 -encoding UTF-8 -bootclasspath "$RT" \
    -d "$TC/classes" \
    /work/tests/VmSuite.java /work/tests/KeyBindTest.java \
    /work/tests/CloseTest.java /work/tests/VersionGateTest.java

# Version-gate fixture: a normal Java 8 class whose class-file major version is
# bumped 52 -> 53 (Java 9), so the VM must reject it with
# UnsupportedClassVersionError.  No JDK 9 needed -- patch one byte.
echo "=== building V9Bomb (class-file major 53) ==="
cat > "$TC/V9Bomb.java" <<'EOF'
public class V9Bomb {
    public static void main(String[] a) {
        System.out.println("V9Bomb RAN - class-version gate FAILED");
    }
}
EOF
"$JAVAC" -source 8 -target 8 -d "$TC/classes" "$TC/V9Bomb.java"
# class-file layout: bytes 0-3 magic, 4-5 minor, 6-7 major (52=0x0034 -> 53=0x0035)
printf '\065' | dd of="$TC/classes/V9Bomb.class" bs=1 seek=7 count=1 conv=notrunc 2>/dev/null
echo "  V9Bomb.class major byte = $(od -An -tu1 -j7 -N1 "$TC/classes/V9Bomb.class" | tr -d ' ') (expect 53)"

(cd "$TC/classes" && "$JAR" cf "$B/testsuite.zip" .)
echo "testsuite.zip OK ($(wc -c < "$B/testsuite.zip") bytes)"

echo "=== compiling examples ==="
for app in HelloJava SwingDemo; do
    rm -rf "$TC/ex-$app"; mkdir -p "$TC/ex-$app"
    "$JAVAC" -source 8 -target 8 -encoding UTF-8 -bootclasspath "$RT" \
        -d "$TC/ex-$app" "/work/examples/$app.java"
    (cd "$TC/ex-$app" && "$JAR" cfe "$EX/$app.jar" "$app" .)
    echo "  $app.jar OK ($(wc -c < "$EX/$app.jar") bytes)"
done
echo "=== build-tests done ==="
