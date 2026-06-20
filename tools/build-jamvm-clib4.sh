#!/bin/sh
# clib4 build + link of JAmiga JamVM (gnuclasspath classlib).
#
# Replaces JAmiga's hand-rolled POSIX shim with clib4: threads/signals/mmap/
# scandir/dlopen come from clib4, so the shim sources (pthread.c, signal.c,
# scandir.c, memory.c, scanprocess.c, time.c, baseline_glue.c) and the shadowing
# shim headers (shelved as *.jamiga-shim-bak) are NOT built. os/amiga/os.c is the
# clib4/dlopen variant; initialisePlatform() lives in os/amiga/powerpc/init.c.
#
# Uses the in-repo clib4/ submodule (built by tools/build-clib4.sh, newer than
# the SDK's): clib4/build/lib + library/include are overlaid onto the container's
# SDK clib4 so -mcrt=clib4 picks it up.
#
#   docker run --rm -v "<proj>:/work" -w /work \
#       javaos4-build:latest sh /work/tools/build-jamvm-clib4.sh
set -e

# --- overlay local clib4 over the SDK clib4 --------------------------------
SDKCLIB4=/opt/ppc-amigaos/ppc-amigaos/SDK/clib4
if [ -d /work/clib4/build/lib ]; then
    cp -f /work/clib4/build/lib/*.a "$SDKCLIB4/lib/" 2>/dev/null || true
    cp -f /work/clib4/build/lib/*.o "$SDKCLIB4/lib/" 2>/dev/null || true
    cp -rf /work/clib4/library/include/* "$SDKCLIB4/include/" 2>/dev/null || true
    echo "=== overlaid in-repo clib4 submodule (clib4/build/lib + library/include) ==="
fi

cd /work/vendor/jamvm/src

# Install the gnuclasspath classlib headers as src/classlib*.h (what configure
# would symlink).  Needed so switching between this and the openjdk build (which
# installs its own) leaves src/ consistent -- `-I .` finds src/classlib*.h first.
for h in classlib.h classlib-defs.h classlib-excep.h classlib-symbol.h; do
    cp -f "classlib/gnuclasspath/$h" "$h"
done

INC="-I . -I os/amiga -I os/amiga/powerpc -I interp -I interp/engine -I classlib/gnuclasspath"
# -DUSE_ZIP enables the real zip reader in zip.c (needed to read glibj.zip /
# jamvmclasses.zip on the boot classpath); otherwise a NULL stub is compiled.
# -DSHARED_CHAR_BUFFERS: REQUIRED for the gnuclasspath classlib (configure.ac sets
# it for gnuclasspath/openjdk6). It makes createString() set java.lang.String's
# 'count'/'offset' fields. Without it, every String's count is 0, so Java-side
# String.hashCode()/equals() see all strings as empty -> the System properties
# Hashtable collapses to a single entry (file.separator etc. read back as garbage),
# breaking File.<clinit>, java.library.path, and native lib loading.
CFLAGS="-mcrt=clib4 -O0 -W -Wall -D__USE_INLINE__ -DUSE_ZIP -DSHARED_CHAR_BUFFERS -fcommon -fgnu89-inline $INC"
OUT=/tmp/build
mkdir -p "$OUT"
OBJS=""
compile() { ppc-amigaos-gcc $CFLAGS -c "$1" -o "$OUT/$2.o"; OBJS="$OBJS $OUT/$2.o"; }

echo "=== core (all src/*.c except dll_ffi.c) ==="
for f in *.c; do
    case "$f" in dll_ffi.c) continue ;; esac
    compile "$f" "$(basename "$f" .c)"
done

echo "=== interpreter ==="
compile interp/direct.c                i_direct
compile interp/inlining.c              i_inlining
compile interp/engine/interp.c         e_interp
compile interp/engine/interp2.c        e_interp2
compile interp/engine/relocatability.c e_reloc

echo "=== classlib (gnuclasspath) ==="
for f in thread class natives excep reflect dll jni properties annotations frame alloc; do
    compile "classlib/gnuclasspath/$f.c" "cl_$f"
done

echo "=== os/amiga (clib4: os.c + arch only) ==="
compile os/amiga/os.c            os_os
compile os/amiga/powerpc/dll_md.c ppc_dll_md
compile os/amiga/powerpc/init.c   ppc_init
ppc-amigaos-gcc $CFLAGS -c os/amiga/powerpc/callNative.S -o "$OUT/ppc_callNative.o"
OBJS="$OBJS $OUT/ppc_callNative.o"

echo "=== linking jamvm (clib4) ==="
DEST=/work/build
mkdir -p "$DEST"
# NOTE: -Wl,--export-dynamic is a NO-OP here -- AmigaOS4 executables are static ELF
# with NO dynamic section, so there is nothing for a dlopen'd .so to resolve against.
# The correct mechanism (clib4) is to link the executable with `-use-dynld` so it
# uses the dynamic linker and shares ONE clib4 (libc.so) with the dlopen'd .so files,
# then ship clib4's sobjs (libc.so/libm.so/libpthread.so/librt.so) with the app and
# rpath the .so to them (do NOT overwrite the system SOBJS:, which holds newlib).
# (native symbol resolution via clib4's shared-library model.)
# -use-dynld: use clib4's dynamic linker so jamvm shares ONE clib4 (libc.so) with
#   the dlopen'd GNU Classpath native .so files (lets them resolve malloc/IExec/...).
# -athread=native: thread model required by the dynamic-linker startup (per clib4's
#   dlopen sample).
# -Wl,-rpath=SYS:Test: where the loader finds the shipped clib4 sobjs (libc.so etc.)
#   at runtime -- deployed alongside jamvm. Do NOT touch the system SOBJS: (newlib).
ppc-amigaos-gcc -mcrt=clib4 -use-dynld -athread=native -Wl,-rpath=SYS:Test \
    -o "$DEST/jamvm" $OBJS -lpthread -lm -lrt -lz -lauto
echo "LINK OK"
ls -la "$DEST/jamvm"
