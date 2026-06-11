#!/bin/sh
# clib4 build + link of JamVM with the OPENJDK 8 classlib (Phase 2).
#
# Variant of build-jamvm-clib4.sh that builds JamVM against src/classlib/openjdk
# instead of src/classlib/gnuclasspath, so it runs the OpenJDK 8 class library
# (Temurin 8 rt.jar) rather than GNU Classpath.  Differences vs the clib4 script:
#   - INC/classlib loop point at classlib/openjdk (16 .c files).
#   - Defines: -DOPENJDK_VERSION=8 -DJSR292 -DJSR308 -DJSR335 -DJSR901
#     (configure.ac sets these for --with-java-runtime-library=openjdk8).
#   - DROP -DSHARED_CHAR_BUFFERS: it's a gnuclasspath/openjdk6-only thing; OpenJDK 8
#     java.lang.String has only value[] (no offset/count), so createString must NOT
#     use the shared-buffer layout.
#   - Output is build/jamvm-openjdk (keeps the working gnuclasspath build/jamvm).
# Everything else (clib4 overlay, -use-dynld, rpath=SYS:Test, libs) is identical.
#
#   docker run --rm -v "<proj>:/work" -v "<clib4>:/clib4" -w /work \
#       javaos4-build:latest sh /work/tools/build-jamvm-openjdk.sh
set -e

# --- overlay local clib4 over the SDK clib4 --------------------------------
SDKCLIB4=/opt/ppc-amigaos/ppc-amigaos/SDK/clib4
if [ -d /clib4/build/lib ]; then
    cp -f /clib4/build/lib/*.a "$SDKCLIB4/lib/" 2>/dev/null || true
    cp -f /clib4/build/lib/*.o "$SDKCLIB4/lib/" 2>/dev/null || true
    cp -rf /clib4/library/include/* "$SDKCLIB4/include/" 2>/dev/null || true
    echo "=== overlaid local clib4 (build/lib + library/include) ==="
fi

cd /work/vendor/jamvm/src

# Install the openjdk classlib headers as src/classlib*.h -- this is what
# configure would symlink for the chosen classlib.  `-I .` resolves
# #include "classlib.h" to src/ FIRST, so these MUST match the classlib we build;
# otherwise the (gnuclasspath) versions left in src/ shadow the openjdk ones
# (e.g. missing the JSR292 MethodHandle/invoke/invokeExact symbols).
echo "=== installing openjdk classlib headers into src/ ==="
for h in classlib.h classlib-defs.h classlib-excep.h classlib-symbol.h; do
    cp -f "classlib/openjdk/$h" "$h"
done

INC="-I . -I os/amiga -I os/amiga/powerpc -I interp -I interp/engine -I classlib/openjdk"
# VERSION_* are JamVM's version (2.0.1), normally from config.h; the openjdk
# classlib's jvm.c (JVM_GetVersionInfo) needs them (gnuclasspath has no jvm.c).
# -fPIC: the VM objects go into the shared libjvm.so (see linking section).
CFLAGS="-mcrt=clib4 -O0 -W -Wall -fPIC -D__USE_INLINE__ -DUSE_ZIP \
 -DOPENJDK_VERSION=8 -DJSR292 -DJSR308 -DJSR335 -DJSR901 \
 -DVERSION_MAJOR=2 -DVERSION_MINOR=0 -DVERSION_MICRO=1 \
 -fcommon -fgnu89-inline $INC"
OUT=/tmp/build-openjdk
rm -rf "$OUT"; mkdir -p "$OUT"
OBJS=""        # libcore objects -> libjvm.so
JAMO=""        # jam.c (main) -> the launcher, links against libjvm.so
compile() { ppc-amigaos-gcc $CFLAGS -c "$1" -o "$OUT/$2.o"; OBJS="$OBJS $OUT/$2.o"; }

echo "=== core (all src/*.c except dll_ffi.c; jam.c -> launcher) ==="
for f in *.c; do
    case "$f" in
        dll_ffi.c) continue ;;
        jam.c) ppc-amigaos-gcc $CFLAGS -c jam.c -o "$OUT/jam.o"; JAMO="$OUT/jam.o"; continue ;;
    esac
    compile "$f" "$(basename "$f" .c)"
done

echo "=== interpreter ==="
compile interp/direct.c                i_direct
compile interp/inlining.c              i_inlining
compile interp/engine/interp.c         e_interp
compile interp/engine/interp2.c        e_interp2
compile interp/engine/relocatability.c e_reloc

echo "=== classlib (openjdk) ==="
for f in thread class natives excep reflect dll jni jvm properties management \
         access frame shutdown alloc perf mh; do
    compile "classlib/openjdk/$f.c" "cl_$f"
done

echo "=== os/amiga (clib4: os.c + arch only) ==="
compile os/amiga/os.c             os_os
compile os/amiga/powerpc/dll_md.c ppc_dll_md
compile os/amiga/powerpc/init.c   ppc_init
ppc-amigaos-gcc $CFLAGS -c os/amiga/powerpc/callNative.S -o "$OUT/ppc_callNative.o"
OBJS="$OBJS $OUT/ppc_callNative.o"

DEST=/work/build
mkdir -p "$DEST"

# libjvm.so = the VM as a SHARED library (upstream's libjvm.la = libcore).  A .so
# exports its defined symbols, so OpenJDK's libjava.so can resolve JVM_*/JNI_*
# against it at dlopen (an executable can't export them on AmigaOS -- see memory
# phase2-openjdk8-scoping).  rpath=SYS:Test for the clib4 sobjs.
echo "=== linking libjvm.so (shared VM) ==="
# Plain -shared (NOT -use-dynld, which forces --no-undefined): leave libc/pthread/
# zlib/JNI undefined -> resolved at runtime against the clib4 sobjs that the
# -use-dynld launcher loads (global symbol scope), same as the classpath natives.
ppc-amigaos-gcc -mcrt=clib4 -shared -fPIC -Wl,-rpath=SYS:Test \
    -o "$DEST/libjvm.so" $OBJS
echo "  libjvm.so OK ($(wc -c < "$DEST/libjvm.so") bytes)"

# AmigaOS $VER: cookie (read by the `Version` command), linked into the
# launcher.  Version proper = the Java-OS4 project version (clean major.minor);
# the OpenJDK class-library version follows as free text.
PVER=$(cut -d. -f1,2 /work/VERSION 2>/dev/null)
[ -n "$PVER" ] || PVER="0.0"
JVER=$(sed -n 's/^JAVA_VERSION="\(.*\)"$/\1/p' /opt/jdk8/release 2>/dev/null)
[ -n "$JVER" ] || JVER="1.8.0"
JDATE=$(date +%d.%m.%Y)
ppc-amigaos-gcc -mcrt=clib4 -O2 -fPIC \
    -DJAVAOS4_VER="\"$PVER\"" -DJAVAOS4_JAVAVER="\"$JVER\"" \
    -DJAVAOS4_DATE="\"$JDATE\"" \
    -c /work/src/version/verstag.c -o "$OUT/verstag.o"

# jamvm launcher = jam.c (main) linked AGAINST libjvm.so -- one shared VM instance
# (so jam.c's main and any dlopen'd libjava see the same VM).
echo "=== linking jamvm-openjdk launcher (-> libjvm.so) ==="
ppc-amigaos-gcc -mcrt=clib4 -use-dynld -athread=native -Wl,-rpath=SYS:Test \
    -o "$DEST/jamvm-openjdk" "$JAMO" "$OUT/verstag.o" -L"$DEST" -ljvm \
    -lpthread -lm -lrt -lz -lauto
echo "LINK OK"
ls -la "$DEST/libjvm.so" "$DEST/jamvm-openjdk"
echo "=== libjvm.so exports JVM_* ? ==="
ppc-amigaos-readelf --dyn-syms "$DEST/libjvm.so" 2>/dev/null | grep -cE "JVM_"
