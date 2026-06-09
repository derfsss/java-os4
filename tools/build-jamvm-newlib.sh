#!/bin/sh
# Baseline newlib build + link of JAmiga JamVM (gnuclasspath classlib).
# Reproduces the object set from JAmiga's hand Makefiles without their
# Amiga-only commands (makelink/delete/bumprev). Run INSIDE the
# amigaos4-gcc11 container with the working dir = vendor/jamvm.
#
#   docker run --rm -v "<root>/vendor/jamvm:/work" -w /work \
#       amigaos4-gcc11:latest sh tools-build/build-jamvm-newlib.sh
#
# (this file is copied in by build.ps1 / the caller; see Phase 0 notes)
set -e
cd src

INC="-I . -I os/amiga -I os/amiga/powerpc -I interp -I interp/engine -I classlib/gnuclasspath"
# Legacy-C / gcc-11 migration flags:
#   -fcommon       : JAmiga defines globals (CondVariable, PthreadInitData, ...)
#                    in headers; gcc 10+ defaults to -fno-common -> multiple defs.
#   -fgnu89-inline : memory.c etc. use bare `inline` (e.g. getMemEntry); C99
#                    inline semantics emit no external symbol -> undefined refs.
CFLAGS="-mcrt=newlib -O0 -W -Wall -D__USE_INLINE__ -fcommon -fgnu89-inline $INC"
OUT=/tmp/build
mkdir -p "$OUT"
OBJS=""

compile() { # <srcfile> <objname>
    ppc-amigaos-gcc $CFLAGS -c "$1" -o "$OUT/$2.o"
    OBJS="$OBJS $OUT/$2.o"
}

echo "=== core (all src/*.c except dll_ffi.c; JAmiga uses the stubs path) ==="
for f in *.c; do
    case "$f" in dll_ffi.c) continue ;; esac   # libffi alt to stubs.c/jni-stubs.c
    compile "$f" "$(basename "$f" .c)"
done

echo "=== interpreter (interp/ + interp/engine/) ==="
compile interp/direct.c               i_direct
compile interp/inlining.c             i_inlining
compile interp/engine/interp.c        e_interp
compile interp/engine/interp2.c       e_interp2
compile interp/engine/relocatability.c e_reloc

echo "=== classlib (gnuclasspath) ==="
for f in thread class natives excep reflect dll jni properties annotations frame alloc; do
    [ -f "classlib/gnuclasspath/$f.c" ] && compile "classlib/gnuclasspath/$f.c" "cl_$f"
done

echo "=== os/amiga ==="
for f in os scandir pthread signal scanprocess time memory baseline_glue; do
    [ -f "os/amiga/$f.c" ] && compile "os/amiga/$f.c" "os_$f"
done

echo "=== os/amiga/powerpc (+ asm) ==="
compile os/amiga/powerpc/dll_md.c ppc_dll_md
compile os/amiga/powerpc/init.c   ppc_init
ppc-amigaos-gcc $CFLAGS -c os/amiga/powerpc/callNative.S -o "$OUT/ppc_callNative.o"
OBJS="$OBJS $OUT/ppc_callNative.o"

echo "=== linking jamvm ==="
# Emit to the mounted project build/ dir so the artifact persists on the host.
DEST=/work/build
mkdir -p "$DEST"
ppc-amigaos-gcc -mcrt=newlib -o "$DEST/jamvm" $OBJS -lauto -lm -lz
echo "LINK OK"
ls -la "$DEST/jamvm"
