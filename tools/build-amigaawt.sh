#!/bin/sh
# Build libamigaawt.so -- the minimal AmigaOS windowing JNI (Phase 4 M3):
# Intuition window + WritePixelArray ARGB blit + IDCMP event polling.
set -e
J=/work/build/openjdk8/jdk-3334efeacd83
OUT=/work/build/openjdk-natives
CC="ppc-amigaos-gcc -mcrt=clib4 -fPIC -O2 -Wall -fcommon"
mkdir -p "$OUT"
$CC -D__USE_BASETYPE__ \
    -I "$J/src/share/javavm/export" -I "$J/src/solaris/javavm/export" \
    -c /work/src/amigaawt/libamigaawt.c -o "$OUT/libamigaawt.o" 2>&1 | head -20
ppc-amigaos-gcc -mcrt=clib4 -fPIC -shared -Wl,-rpath=SYS:Test \
    -o "$OUT/libamigaawt.so" "$OUT/libamigaawt.o"
echo "libamigaawt.so OK ($(wc -c < "$OUT/libamigaawt.so") bytes)"
ppc-amigaos-nm -D -u "$OUT/libamigaawt.so" 2>/dev/null | grep -vE "IExec|__|Jam_|JNI" | head -5
