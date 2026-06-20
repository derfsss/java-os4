#!/bin/sh
# Build clib4 from the in-repo submodule (clib4/) with its GNUmakefile.os4 build,
# producing clib4/build/{clib4.library, lib/*.a, lib/*.o, lib/*.so}. The VM and
# native build scripts overlay these onto the SDK clib4 (-mcrt=clib4) and
# package.sh ships clib4.library + the .so front-ends.
#
#   docker run --rm -v "<proj>:/work" -w /work javaos4-build:latest \
#       sh /work/tools/build-clib4.sh
set -e
cd /work/clib4
echo "=== building clib4 $(git rev-parse --short HEAD 2>/dev/null || echo '?') ==="
make -f GNUmakefile.os4

echo "=== clib4 build outputs ==="
ls -l build/clib4.library
echo "  static libs: $(ls build/lib/*.a 2>/dev/null | wc -l)   shared objs: $(ls build/lib/*.so 2>/dev/null | wc -l)"
ls build/lib/*.a build/lib/*.so 2>/dev/null | sed 's,^,    ,'

# gitver stamps library/c.lib_rev.h with the git hash; restore it so the
# submodule working tree stays clean for `git status`.
git checkout -- library/c.lib_rev.h 2>/dev/null || true
echo "=== build-clib4 done ==="
