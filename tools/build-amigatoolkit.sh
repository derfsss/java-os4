#!/bin/sh
# Build the sun.awt.amiga AWT toolkit (Phase 4 M4) -> amigatoolkit.zip,
# and the SwingTest app -> swingtest.zip.
# Compiles against the Temurin 8 rt.jar (the runtime class library).
set -e
JDK8=/opt/jdk8
RT="$JDK8/jre/lib/rt.jar"
SRC=/work/src/amigaawt/java
OUT=/work/build/amigatoolkit
mkdir -p "$OUT/classes" "$OUT/testclasses"

"$JDK8/bin/javac" -source 8 -target 8 -encoding UTF-8 \
    -bootclasspath "$RT" -XDignore.symbol.file=true \
    -d "$OUT/classes" \
    "$SRC"/sun/awt/amiga/*.java

(cd "$OUT/classes" && "$JDK8/bin/jar" cf /work/build/amigatoolkit.zip sun)
echo "amigatoolkit.zip OK ($(wc -c < /work/build/amigatoolkit.zip) bytes)"

"$JDK8/bin/javac" -source 8 -target 8 -encoding UTF-8 \
    -bootclasspath "$RT" \
    -d "$OUT/testclasses" \
    /work/tests/SwingTest.java /work/tests/SwingType.java /work/tests/SwingResize.java /work/tests/SwingApp.java /work/tests/SwingDialog.java

(cd "$OUT/testclasses" && "$JDK8/bin/jar" cf /work/build/swingtest.zip .)
echo "swingtest.zip OK ($(wc -c < /work/build/swingtest.zip) bytes)"

# injin: guest-side input injector (real-input GUI testing)
ppc-amigaos-gcc -mcrt=clib4 -O2 -Wall -o /work/build/injin \
    /work/src/tools/injin.c
echo "injin OK ($(wc -c < /work/build/injin) bytes)"
