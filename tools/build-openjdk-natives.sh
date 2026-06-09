#!/bin/sh
# Cross-compile OpenJDK 8 core native libraries for AmigaOS 4 / clib4 (Phase 2,
# step 2).  Same model as the GNU Classpath natives: ppc-amigaos-gcc, -fPIC, and
# the -use-dynld shared-clib4 scheme (rpath=SYS:Test; ship clib4 sobjs with the app).
#
# OpenJDK has no src/amigaos/native, so the OS-specific (md) code is based on
# src/solaris (unix) and adapted per-file as clib4 gaps surface -- like the
# Classpath VMFile work, at larger scale.
#
# Starts with the PORTABLE deps that libjava links against (validate the pipeline):
#   libfdlibm.a  (pure C math, src/share/native/java/lang/fdlibm)
#   libverify.so (bytecode verifier, src/share/native/common/check_{code,format}.c)
# Then libjava / libzip / ... are added incrementally.
#
#   docker run --rm -v "<proj>:/work" -v "<clib4>:/clib4" -w /work \
#       javaos4-build:latest sh /work/tools/build-openjdk-natives.sh
set -e

SDKCLIB4=/opt/ppc-amigaos/ppc-amigaos/SDK/clib4
if [ -d /clib4/build/lib ]; then
    cp -f /clib4/build/lib/*.a /clib4/build/lib/*.o "$SDKCLIB4/lib/" 2>/dev/null || true
    cp -rf /clib4/library/include/* "$SDKCLIB4/include/" 2>/dev/null || true
fi

J=/work/build/openjdk8/jdk-3334efeacd83
OUT=/work/build/openjdk-natives
mkdir -p "$OUT"
# -fcommon: OpenJDK (like the 2016 JamVM tree) defines globals in headers without
# extern (e.g. parentPathv); gcc 10+ defaults to -fno-common -> multiple-definition.
CC="ppc-amigaos-gcc -mcrt=clib4 -fPIC -O2 -w -fcommon"

# Compatibility shims for unix headers clib4 lacks (OpenJDK's md code targets unix;
# amigaos/clib4 differs).  Kept here (not by editing OpenJDK source) so the drop
# stays pristine.  Add more as gaps surface.
COMPAT="$OUT/compat"
mkdir -p "$COMPAT/sys"
echo '#include <signal.h>' > "$COMPAT/sys/signal.h"   # clib4 has <signal.h>, no <sys/signal.h>
# String-literal defines OpenJDK's makefiles pass via -D (ARCHPROPNAME, version);
# put them in a -include header so the C-string quoting survives cleanly.
cat > "$COMPAT/jdkdefs.h" <<'EOF'
#ifndef JDKDEFS_H
#define JDKDEFS_H
#ifndef ARCHPROPNAME
#define ARCHPROPNAME "ppc"
#endif
#define JDK_MAJOR_VERSION "1"
#define JDK_MINOR_VERSION "8"
#define JDK_MICRO_VERSION "0"
#define JDK_BUILD_NUMBER "b03"
#define JDK_UPDATE_VERSION "77"
#ifndef RELEASE
#define RELEASE "amigaos4"   /* os.version string (normally uname -r) */
#endif
#endif
EOF

# --- source adaptations for amigaos/clib4 (idempotent; keep additive) ---------
# jdk_util_md.h: add an amigaos branch to its OS #if ladder (ISNANF/ISNAND),
# else it #errors "missing platform-specific definition" -> blocks ~7 libjava .c.
MDU="$J/src/solaris/native/common/jdk_util_md.h"
if [ -f "$MDU" ] && ! grep -q __amigaos4__ "$MDU"; then
    sed -i 's|#elif defined(_AIX)|#elif defined(__amigaos4__)\n#include <math.h>\n#define ISNANF(f) isnan(f)\n#define ISNAND(d) isnan(d)\n#elif defined(_AIX)|' "$MDU"
    echo "=== adapted jdk_util_md.h (amigaos ISNANF/ISNAND) ==="
fi

# LFS: clib4 lacks struct stat64/dirent64/flock64 (it has 64-bit base APIs), so
# join OpenJDK's _ALLBSD_SOURCE path that maps the *64 names to base.  Extend the
# guards in the affected md files with __amigaos4__.
for f in "$J/src/solaris/native/java/io/io_util_md.h" \
         "$J/src/solaris/native/java/io/UnixFileSystem_md.c" \
         "$J/src/solaris/native/java/lang/childproc.c" \
         "$J/src/solaris/native/java/util/TimeZone_md.c" \
         "$J/src/solaris/native/java/util/FileSystemPreferences.c"; do
    [ -f "$f" ] || continue
    if ! grep -q __amigaos4__ "$f"; then
        sed -i 's@#ifdef _ALLBSD_SOURCE@#if defined(_ALLBSD_SOURCE) || defined(__amigaos4__)@g; s@defined(_ALLBSD_SOURCE)@(defined(_ALLBSD_SOURCE) || defined(__amigaos4__))@g' "$f"
        echo "=== adapted $(basename "$f") (amigaos LFS->base) ==="
    fi
done

# io_util_md.c: FIONREAD lives in clib4's <sys/filio.h>, included only under the
# __solaris__ guard; extend it (and the _ALLBSD ioctl guard) for amigaos.
IOMD="$J/src/solaris/native/java/io/io_util_md.c"
if [ -f "$IOMD" ] && ! grep -q __amigaos4__ "$IOMD"; then
    sed -i 's@#ifdef __solaris__@#if defined(__solaris__) || defined(__amigaos4__)@g; s@defined(_ALLBSD_SOURCE)@(defined(_ALLBSD_SOURCE) || defined(__amigaos4__))@g' "$IOMD"
    echo "=== adapted io_util_md.c (amigaos FIONREAD via sys/filio.h) ==="
fi

# UnixFileSystem_md.c statMode(): AmigaDOS relative paths carry NO "./" current-dir
# prefix; clib4 passes "./X" straight to Lock() which fails.  Strip a leading "./" so
# File.exists()/loadLibrary() resolve CWD-relative files (e.g. ./libzip.so during
# System.initializeSystemClass loadLibrary("zip")).  Idempotent.
UFS="$J/src/solaris/native/java/io/UnixFileSystem_md.c"
if [ -f "$UFS" ] && ! grep -q "path += 2" "$UFS"; then
    perl -0pi -e 's/(statMode\(const char \*path, int \*mode\)\s*\{\n\s*struct stat64 sb;\n)/$1#ifdef __amigaos4__\n    if (path[0] == \x27.\x27 && path[1] == \x27\/\x27) path += 2;  \/* AmigaDOS has no .\/ prefix *\/\n#endif\n/' "$UFS"
    echo "=== adapted UnixFileSystem_md.c statMode (strip leading ./) ==="
fi

# Temurin 8 (late 8u) renamed FileInputStream.available -> available0 (JDK-8080679);
# the 8u77 native drop still names it Java_..._available -> UnsatisfiedLinkError at
# runtime (rt.jar calls available0).  Rename to match the runtime class library.
FIS="$J/src/share/native/java/io/FileInputStream.c"
if [ -f "$FIS" ] && ! grep -q "FileInputStream_available0" "$FIS"; then
    sed -i 's/Java_java_io_FileInputStream_available(/Java_java_io_FileInputStream_available0(/' "$FIS"
    echo "=== adapted FileInputStream.c available->available0 (Temurin skew) ==="
fi

# TimeZone_md.c: treat amigaos like __linux__ for the tz-file detection blocks (they
# fopen /etc/localtime etc. -> NULL on Amiga -> graceful fallback) and like MACOSX for
# getGMTOffsetID (uses struct tm.tm_gmtoff, which clib4 HAS; the plain branch uses the
# SysV `timezone` global, which clib4 LACKS).  3 targeted guards.  Idempotent.
TZMD="$J/src/solaris/native/java/util/TimeZone_md.c"
if [ -f "$TZMD" ] && ! grep -q "defined(MACOSX) || defined(__amigaos4__)" "$TZMD"; then
    sed -i \
      -e 's@#if defined(__linux__) || defined(MACOSX) || defined(__solaris__)@#if defined(__linux__) || defined(MACOSX) || defined(__solaris__) || defined(__amigaos4__)@' \
      -e 's@#if defined(__linux__) || defined(MACOSX)$@#if defined(__linux__) || defined(MACOSX) || defined(__amigaos4__)@' \
      -e 's@^#if defined(MACOSX)$@#if defined(MACOSX) || defined(__amigaos4__)@' \
      "$TZMD"
    echo "=== adapted TimeZone_md.c (amigaos -> linux tz-files + tm_gmtoff GMT offset) ==="
fi

# OpenJDK export headers (jni.h/jvm.h) + per-OS jni_md.h/jvm_md.h (solaris=unix) +
# the shared native common headers (jni_util.h, jlong.h, ...) + the compat shims.
EXP="-I $COMPAT -I $J/src/share/javavm/export -I $J/src/solaris/javavm/export -I $J/src/share/native/common"

echo "=== libfdlibm.a (pure C math) ==="
FD=$J/src/share/native/java/lang/fdlibm
fdobjs=""
for c in "$FD"/src/*.c; do
    o="$OUT/fd_$(basename "$c" .c).o"
    if $CC -I "$FD/include" -c "$c" -o "$o" 2>"$OUT/e"; then
        fdobjs="$fdobjs $o"
    else
        echo "  FDLIBM FAIL $(basename "$c")"; head -4 "$OUT/e"
    fi
done
ppc-amigaos-ar rcs "$OUT/libfdlibm.a" $fdobjs
echo "  libfdlibm.a OK ($(ppc-amigaos-ar t "$OUT/libfdlibm.a" | wc -l) objects)"

echo "=== libverify.so (bytecode verifier) ==="
C=$J/src/share/native/common
ok=1
for f in check_code check_format; do
    if ! $CC $EXP -c "$C/$f.c" -o "$OUT/$f.o" 2>"$OUT/e"; then
        echo "  VERIFY FAIL $f.c"; head -8 "$OUT/e"; ok=0
    fi
done
if [ $ok = 1 ]; then
    ppc-amigaos-gcc -mcrt=clib4 -fPIC -shared -Wl,-rpath=SYS:Test \
        -o "$OUT/libverify.so" "$OUT/check_code.o" "$OUT/check_format.o" 2>"$OUT/e" \
        && echo "  libverify.so OK ($(wc -c < "$OUT/libverify.so") bytes)" \
        || { echo "  libverify.so LINK FAIL"; head -8 "$OUT/e"; }
fi

echo "=== javah: generate JNI headers (from Temurin rt.jar) ==="
HDR="$OUT/headers"; mkdir -p "$HDR"
RTJAR=/opt/jdk8/jre/lib/rt.jar
/opt/jdk8/bin/javah -d "$HDR" -classpath "$RTJAR" \
  java.io.Console java.io.FileDescriptor java.io.FileInputStream java.io.FileOutputStream \
  java.io.FileSystem java.io.ObjectInputStream java.io.ObjectOutputStream \
  java.io.ObjectStreamClass java.io.RandomAccessFile java.io.UnixFileSystem \
  java.lang.Class java.lang.ClassLoader 'java.lang.ClassLoader$NativeLibrary' \
  java.lang.Compiler java.lang.Double java.lang.Float java.lang.Object java.lang.Package \
  java.lang.reflect.Array java.lang.reflect.Executable java.lang.reflect.Field \
  java.lang.reflect.Proxy java.lang.Runtime java.lang.SecurityManager java.lang.Shutdown \
  java.lang.StrictMath java.lang.String java.lang.System java.lang.Thread java.lang.Throwable \
  java.security.AccessController java.util.concurrent.atomic.AtomicLong java.util.jar.JarFile \
  java.util.TimeZone java.util.zip.Adler32 java.util.zip.CRC32 java.util.zip.Deflater \
  java.util.zip.Inflater java.util.zip.ZipFile \
  sun.misc.GC sun.misc.MessageUtils sun.misc.NativeSignalHandler sun.misc.Signal \
  sun.misc.URLClassPath sun.misc.Version sun.misc.VM sun.misc.VMSupport \
  sun.reflect.ConstantPool sun.reflect.Reflection \
  sun.reflect.NativeConstructorAccessorImpl sun.reflect.NativeMethodAccessorImpl 2>&1 | tail -3
echo "  generated $(ls "$HDR" 2>/dev/null | wc -l) headers"

echo "=== libjava: compile pass (collect adaptation list) ==="
LJDIRS="solaris/native/java/lang share/native/java/lang share/native/java/lang/reflect \
 share/native/java/io solaris/native/java/io share/native/java/nio share/native/java/security \
 share/native/common share/native/sun/misc share/native/sun/reflect share/native/java/util \
 share/native/java/util/concurrent/atomic solaris/native/common solaris/native/java/util"
LJINC="-I $HDR -I $J/src/share/native/java/lang/fdlibm/include $EXP -include $COMPAT/jdkdefs.h"
for d in $LJDIRS; do LJINC="$LJINC -I $J/src/$d"; done
# TimeZone_md.c IS built now (the amigaos guards below treat it like __linux__ for
# tz-file detection -- /etc/localtime etc. just don't exist on Amiga so it falls back
# -- and like MACOSX for getGMTOffsetID, which uses struct tm.tm_gmtoff that clib4 has,
# instead of the SysV `timezone`/`altzone` globals it lacks).
EXCL="check_code.c check_format.c jspawnhelper.c java_props_macosx.c"
mkdir -p "$OUT/libjava"
ok=0; fail=0
for d in $LJDIRS; do
    for c in "$J/src/$d"/*.c; do
        [ -f "$c" ] || continue
        b=$(basename "$c")
        case " $EXCL " in *" $b "*) continue ;; esac
        # TimeZone_md.c needs struct tm.tm_gmtoff, which clib4 gates behind
        # _GNU_SOURCE/_BSD_SOURCE/_XOPEN_SOURCE (raw member is __tm_gmtoff).
        extra=""; [ "$b" = "TimeZone_md.c" ] && extra="-D_GNU_SOURCE"
        if $CC $extra $LJINC -c "$c" -o "$OUT/libjava/$(basename "$c" .c).o" 2>"$OUT/e"; then
            ok=$((ok+1))
        else
            fail=$((fail+1)); echo "  FAIL $d/$b"
            grep -m1 -E "error:|No such file" "$OUT/e" | sed 's/^/        /'
        fi
    done
done
echo "  libjava compile: $ok OK, $fail FAILED"

echo "=== link libjava.so ==="
# Recipe (CoreLibraries.gmk): libjava links -lverify + static libfdlibm.  The VM
# (JVM_*) symbols come from jamvm at runtime (-use-dynld), so leave them undefined
# here (shared objects allow it).  rpath=SYS:Test for the clib4 sobjs.
if ppc-amigaos-gcc -mcrt=clib4 -fPIC -shared -Wl,-rpath=SYS:Test \
       -o "$OUT/libjava.so" "$OUT"/libjava/*.o "$OUT/libfdlibm.a" \
       -L"$OUT" -lverify 2>"$OUT/e"; then
    echo "  libjava.so OK ($(wc -c < "$OUT/libjava.so") bytes)"
else
    echo "  libjava.so LINK FAIL"; head -20 "$OUT/e"
fi

echo "=== libzip.so (java.util.zip + bundled zlib-1.2.8) ==="
# System.initializeSystemClass() calls loadLibrary("zip") -> libzip.so must exist
# on java.library.path (= sun.boot.library.path = SYS:Test).  OpenJDK bundles
# zlib-1.2.8 (USE_EXTERNAL_LIBZ=false default), so compile it in -> self-contained
# (no dependency on clib4's libz version).  JNU_*/jio_* come from libjava at runtime
# (leave undefined, like libjava leaves JVM_* undefined).
ZIP=$J/src/share/native/java/util/zip
ZLIB=$ZIP/zlib-1.2.8
# Temurin 8 (a late 8u) dropped the addSlash arg from ZipFile.getEntry; the 8u77
# IcedTea native drop still has it -> signature clash with the javah header from
# Temurin's rt.jar.  Match the runtime class library: drop addSlash, pass JNI_FALSE
# to ZIP_GetEntry2 (later 8u handles the trailing-slash retry in Java). Idempotent.
ZF=$ZIP/ZipFile.c
if [ -f "$ZF" ] && grep -q "jbyteArray name, jboolean addSlash)" "$ZF"; then
    sed -i 's@jbyteArray name, jboolean addSlash)@jbyteArray name)@; s@ZIP_GetEntry2(zip, path, (jint)ulen, addSlash)@ZIP_GetEntry2(zip, path, (jint)ulen, JNI_FALSE)@' "$ZF"
    echo "=== adapted ZipFile.c getEntry (drop addSlash to match Temurin rt.jar) ==="
fi
ZINC="-I $HDR -I $ZIP -I $ZLIB $EXP -I $J/src/solaris/native/common \
 -I $J/src/share/native/java/io -I $J/src/solaris/native/java/io -include $COMPAT/jdkdefs.h"
mkdir -p "$OUT/libzip"
zok=0; zfail=0
for c in "$ZLIB"/*.c "$ZIP"/Adler32.c "$ZIP"/CRC32.c "$ZIP"/Deflater.c \
         "$ZIP"/Inflater.c "$ZIP"/zip_util.c "$ZIP"/ZipFile.c; do
    [ -f "$c" ] || continue
    if $CC $ZINC -c "$c" -o "$OUT/libzip/$(basename "$c" .c).o" 2>"$OUT/e"; then
        zok=$((zok+1))
    else
        zfail=$((zfail+1)); echo "  ZIP FAIL $(basename "$c")"
        grep -m1 -E "error:|No such file" "$OUT/e" | sed 's/^/        /'
    fi
done
echo "  libzip compile: $zok OK, $zfail FAILED"

# ZipFile.getManifestNum: native added in later 8u (security: count META-INF/
# MANIFEST.MF entries); absent from the 8u77 source but Temurin's rt.jar calls it ->
# UnsatisfiedLinkError.  Supply it (0/1 via ZIP_GetEntry -- normal jars have exactly
# one canonical MANIFEST.MF), in a compat .c compiled into libzip.so.
cat > "$OUT/libzip/zip_manifest_compat.c" <<'ZEOF'
#include "jni.h"
#include "jlong.h"
#include "zip_util.h"
JNIEXPORT jint JNICALL
Java_java_util_zip_ZipFile_getManifestNum(JNIEnv *env, jclass cls, jlong zfile) {
    jzfile *zip = jlong_to_ptr(zfile);
    jzentry *ze = ZIP_GetEntry(zip, "META-INF/MANIFEST.MF", 0);
    jint n = (ze != NULL) ? 1 : 0;
    if (ze != NULL) ZIP_FreeEntry(zip, ze);
    return n;
}
ZEOF
if $CC $ZINC -c "$OUT/libzip/zip_manifest_compat.c" -o "$OUT/libzip/zip_manifest_compat.o" 2>"$OUT/e"; then
    echo "  getManifestNum compat OK"
else
    echo "  getManifestNum compat FAIL"; head -6 "$OUT/e"
fi

if ppc-amigaos-gcc -mcrt=clib4 -fPIC -shared -Wl,-rpath=SYS:Test \
       -o "$OUT/libzip.so" "$OUT"/libzip/*.o 2>"$OUT/e"; then
    echo "  libzip.so OK ($(wc -c < "$OUT/libzip.so") bytes)"
else
    echo "  libzip.so LINK FAIL"; head -20 "$OUT/e"
fi

echo "=== built ==="; ls -l "$OUT"/*.a "$OUT"/*.so 2>/dev/null