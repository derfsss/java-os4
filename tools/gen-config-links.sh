#!/bin/sh
# Generate the per-config header "links" that JamVM's ./configure normally
# creates via AC_CONFIG_LINKS (configure.ac). JAmiga's Amiga port builds with
# hand-written Makefiles and skips ./configure, so we reproduce the links here.
# We copy (not symlink) for Windows-checkout safety.
#
# Usage: tools/gen-config-links.sh [arch] [classlib]
#        arch     default: powerpc
#        classlib default: gnuclasspath   (use 'openjdk' for the OpenJDK 8 build)
set -e
ARCH="${1:-powerpc}"
CLASSLIB="${2:-gnuclasspath}"
SRC="$(cd "$(dirname "$0")/../vendor/jamvm/src" && pwd)"

cp -f "$SRC/arch/$ARCH.h" "$SRC/arch.h"
for h in classlib classlib-defs classlib-symbol classlib-excep; do
    cp -f "$SRC/classlib/$CLASSLIB/$h.h" "$SRC/$h.h"
done
echo "Generated in $SRC: arch.h (<- $ARCH), classlib*.h (<- $CLASSLIB)"
