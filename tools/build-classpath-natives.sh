#!/bin/sh
# Direct cross-build of GNU Classpath core native libraries as clib4 ELF .so.
# (Classpath's libtool only emits .a on amigaos, which dlopen can't load, so we
# compile/link .so directly.) Requires the tree already cross-configured for
# host=powerpc-unknown-amigaos so include/config.h is the big-endian AmigaOS one.
# Run inside javaos4-build with /work=project (clib4 is the in-repo clib4/ submodule).
#
# Builds the core set needed for headless Hello World: javalang, javalangreflect,
# javaio, javanio, javautil. Skips peers, networking (cpnet gethostbyname_r),
# and Linux/BSD-only nio (epoll/kqueue) and iconv.
SDKCLIB4=/opt/ppc-amigaos/ppc-amigaos/SDK/clib4
cp -f /work/clib4/build/lib/*.a /work/clib4/build/lib/*.o "$SDKCLIB4/lib/" 2>/dev/null || true
cp -rf /work/clib4/library/include/* "$SDKCLIB4/include/" 2>/dev/null || true

CP=/work/vendor/fallback/classpath
J=$CP/native/jni
INC="-I $CP/include -I $J/classpath -I $J/native-lib"
CF="-mcrt=clib4 -fPIC -O2 -DHAVE_CONFIG_H -fexceptions -w $INC"
OUT=/work/build/cpnatives
mkdir -p "$OUT"

cc1() { # <src> <obj>  -> compile, return status
    ppc-amigaos-gcc $CF -I "$(dirname "$1")" -c "$1" -o "$2" 2>"$OUT/e"
}

# Helper objects linked into every native lib (JCL + portable native layer).
HELP=""
for h in classpath/jcl.c classpath/jnilink.c native-lib/cpio.c native-lib/cpproc.c; do
    o="$OUT/$(basename "$h" .c).o"
    if cc1 "$J/$h" "$o"; then HELP="$HELP $o"; else echo "HELPER FAIL $h"; head -4 "$OUT/e"; fi
done

mklib() { # <name> <src...>
    name=$1; shift
    objs=""
    for c in "$@"; do
        o="$OUT/$(basename "$c" .c).o"
        if cc1 "$c" "$o"; then objs="$objs $o"; else echo "  [lib$name] compile FAIL $(basename "$c")"; head -3 "$OUT/e"; fi
    done
    # -Wl,-rpath=SYS:Test so the .so finds the shipped clib4 libc.so etc. at load;
    # -mcrt=clib4 on a -shared link records clib4's libc.so as a NEEDED dependency
    # (mirrors clib4's dlopen sample) so undefined libc/IExec refs bind at load time.
    if ppc-amigaos-gcc -mcrt=clib4 -fPIC -shared -Wl,-rpath=SYS:Test -o "$OUT/lib$name.so" $objs $HELP 2>"$OUT/e"; then
        echo "  lib$name.so OK ($(wc -c < "$OUT/lib$name.so") bytes)"
    else
        echo "  lib$name.so LINK FAIL"; head -6 "$OUT/e"
    fi
}

echo "=== javalang ===";        mklib javalang $J/java-lang/java_lang_VMSystem.c $J/java-lang/java_lang_VMDouble.c $J/java-lang/java_lang_VMFloat.c $J/java-lang/java_lang_VMMath.c $J/java-lang/java_lang_VMProcess.c
echo "=== javalangreflect ==="; mklib javalangreflect $J/java-lang/java_lang_reflect_VMArray.c
echo "=== javaio ===";          mklib javaio $J/java-io/java_io_VMConsole.c $J/java-io/java_io_VMFile.c $J/java-io/java_io_VMObjectInputStream.c $J/java-io/java_io_VMObjectStreamClass.c
echo "=== javanio ===";         mklib javanio $J/java-nio/gnu_java_nio_VMChannel.c $J/java-nio/gnu_java_nio_VMSelector.c $J/java-nio/gnu_java_nio_VMPipe.c $J/java-nio/java_nio_VMDirectByteBuffer.c $J/java-nio/java_nio_MappedByteBufferImpl.c $J/java-nio/javanio.c
echo "=== javautil ===";        mklib javautil $J/java-util/java_util_VMTimeZone.c

echo "=== built .so ==="; ls -l "$OUT"/*.so 2>/dev/null
