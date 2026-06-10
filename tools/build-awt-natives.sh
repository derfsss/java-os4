#!/bin/sh
# Cross-compile OpenJDK 8 libawt for AmigaOS 4 / clib4 (Phase 4, M1: headless
# Java2D).  Builds the portable libawt set from make/lib/Awt2dLibraries.gmk
# (software loops, image rasters, Region/SurfaceData, pisces support natives) --
# enough for headless BufferedImage + Graphics2D.  X11/OGL (libawt_xawt /
# libawt_headless) and medialib transforms (libmlib_image, dlopen'd; absence is
# handled gracefully by awt_Mlib) are NOT built.
#
# Run with the test config:
#   jamvm-openjdk -Djava.awt.headless=true \
#     -Djava.awt.graphicsenv=sun.awt.X11GraphicsEnvironment \
#     -Dawt.toolkit=sun.awt.HToolkit -cp awttest.zip AwtTest
#
#   docker run --rm -v "<proj>:/work" -v "<clib4>:/clib4" -w /work \
#       javaos4-build:latest sh /work/tools/build-awt-natives.sh
set -e

SDKCLIB4=/opt/ppc-amigaos/ppc-amigaos/SDK/clib4
if [ -d /clib4/build/lib ]; then
    cp -f /clib4/build/lib/*.a /clib4/build/lib/*.o "$SDKCLIB4/lib/" 2>/dev/null || true
    cp -rf /clib4/library/include/* "$SDKCLIB4/include/" 2>/dev/null || true
fi

J=/work/build/openjdk8/jdk-3334efeacd83
OUT=/work/build/openjdk-natives
COMPAT="$OUT/compat"
HDR="$OUT/headers"
RTJAR=/opt/jdk8/jre/lib/rt.jar
CC="ppc-amigaos-gcc -mcrt=clib4 -fPIC -O2 -w -fcommon"
mkdir -p "$OUT/libawt" "$HDR"

SH=$J/src/share/native/sun
SOL=$J/src/solaris/native/sun

AWTINC="-I $HDR -I $COMPAT \
 -I $J/src/share/javavm/export -I $J/src/solaris/javavm/export \
 -I $J/src/share/native/common -I $J/src/solaris/native/common \
 -I $SH/awt -I $SOL/awt \
 -I $SH/awt/image -I $SH/awt/image/gif -I $SH/awt/image/cvutils \
 -I $SH/awt/medialib -I $SH/awt/debug -I $SH/awt/utility \
 -I $SH/java2d -I $SOL/java2d -I $SH/java2d/loops -I $SH/java2d/pipe \
 -I $SH/java2d/opengl -I $SOL/java2d/opengl -I $SH/font \
 -include $COMPAT/jdkdefs.h"
DEFS="-D__MEDIALIB_OLD_NAMES -D__USE_J2D_NAMES -DHEADLESS=1"

# The LIBAWT_FILES list from Awt2dLibraries.gmk (portable subset; the unix
# extras awt_LoadLibrary.c/initIDs.c/img_colors.c are evaluated empirically --
# awt_LoadLibrary chain-loads xawt/headless which we don't want, so EXCLUDED).
FILES="gifdecoder.c imageInitIDs.c img_globals.c SurfaceData.c Region.c
BufImgSurfaceData.c Disposer.c Trace.c GraphicsPrimitiveMgr.c Blit.c BlitBg.c
ScaledBlit.c FillRect.c FillSpans.c FillParallelogram.c DrawParallelogram.c
DrawLine.c DrawRect.c DrawPolygons.c DrawPath.c FillPath.c ProcessPath.c
MaskBlit.c MaskFill.c TransformHelper.c AlphaMath.c AlphaMacros.c AnyByte.c
ByteBinary1Bit.c ByteBinary2Bit.c ByteBinary4Bit.c ByteIndexed.c ByteGray.c
Index8Gray.c Index12Gray.c AnyShort.c Ushort555Rgb.c Ushort565Rgb.c
Ushort4444Argb.c Ushort555Rgbx.c UshortGray.c UshortIndexed.c Any3Byte.c
ThreeByteBgr.c AnyInt.c IntArgb.c IntArgbPre.c IntArgbBm.c IntRgb.c IntBgr.c
IntRgbx.c Any4Byte.c FourByteAbgr.c FourByteAbgrPre.c BufferedMaskBlit.c
BufferedRenderPipe.c ShapeSpanIterator.c SpanClipRenderer.c awt_ImageRep.c
awt_ImagingLib.c awt_Mlib.c awt_parseImage.c DataBufferNative.c dither.c
debug_assert.c debug_mem.c debug_trace.c debug_util.c initIDs.c img_colors.c
MapAccelFunc.c"

# locate each file across the include dirs
findsrc() {
    for d in "$SH/awt" "$SOL/awt" "$SH/awt/image" "$SH/awt/image/gif" \
             "$SH/awt/image/cvutils" "$SH/awt/medialib" "$SH/awt/debug" \
             "$SH/awt/utility" "$SH/java2d" "$SOL/java2d" "$SH/java2d/loops" \
             "$SH/java2d/pipe" "$SH/font"; do
        [ -f "$d/$1" ] && { echo "$d/$1"; return 0; }
    done
    return 1
}

# rect.h: the unix variant typedefs RECT_T = XRectangle (X11/Xlib.h); the MACOSX
# branch is a plain struct -- use that on amiga (no X11).  Idempotent.
RECTH="$SOL/awt/utility/rect.h"
if [ -f "$RECTH" ] && ! grep -q __amigaos4__ "$RECTH"; then
    sed -i 's@#ifndef MACOSX@#if !defined(MACOSX) \&\& !defined(__amigaos4__)@' "$RECTH"
    echo "  adapted rect.h (no X11 RECT_T)"
fi

echo "=== javah (classes derived from the sources' own JNI-header includes) ==="
SRCLIST=""
for f in $FILES; do
    s=$(findsrc "$f") && SRCLIST="$SRCLIST $s"
done
# scan the package headers too -- transitive JNI-header includes
# (e.g. GraphicsPrimitiveMgr.h -> java_awt_AlphaComposite.h)
for d in "$SH/java2d" "$SH/java2d/loops" "$SH/java2d/pipe" "$SH/awt/image" \
         "$SH/awt/image/cvutils" "$SH/awt/medialib" "$SOL/awt" "$SOL/java2d"; do
    for h in "$d"/*.h; do [ -f "$h" ] && SRCLIST="$SRCLIST $h"; done
done
CLASSES=$(cat $SRCLIST 2>/dev/null \
    | grep -ohE '"(java|sun)_[A-Za-z0-9_]+\.h"' \
    | sed 's/"//g; s/\.h$//; s/_/./g' | sort -u)
hok=0; hfail=0
for c in $CLASSES; do
    if /opt/jdk8/bin/javah -d "$HDR" -classpath "$RTJAR" "$c" >/dev/null 2>&1; then
        hok=$((hok+1))
    else
        hfail=$((hfail+1))
    fi
done
echo "  javah: $hok generated, $hfail skipped (non-class headers)"

echo "=== compile libawt ==="
aok=0; afail=0
for f in $FILES; do
    s=$(findsrc "$f") || { echo "  MISSING $f"; afail=$((afail+1)); continue; }
    if $CC $DEFS $AWTINC -c "$s" -o "$OUT/libawt/$(basename "$f" .c).o" 2>"$OUT/e"; then
        aok=$((aok+1))
    else
        afail=$((afail+1)); echo "  AWT FAIL $f"
        grep -m2 -E "error:|No such file" "$OUT/e" | sed 's/^/        /'
    fi
done
echo "  libawt compile: $aok OK, $afail FAILED"

# awt_LoadLibrary.c is EXCLUDED (it chain-loads libawt_xawt/libawt_headless),
# but it defines the global `JavaVM *jvm` that Trace/Disposer/J2D code reads,
# and its JNI_OnLoad sets it.  Provide the minimal equivalent.
cat > "$OUT/libawt/awt_onload_compat.c" <<'OEOF'
#include "jni.h"
JavaVM *jvm = NULL;
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
    jvm = vm;
    return JNI_VERSION_1_2;
}
OEOF
$CC $AWTINC -c "$OUT/libawt/awt_onload_compat.c" -o "$OUT/libawt/awt_onload_compat.o" 2>"$OUT/e" \
    && echo "  awt OnLoad/jvm compat OK" || { echo "  awt OnLoad compat FAIL"; head -3 "$OUT/e"; }

# java.awt.Font.initIDs lives in awt_Font.c (the X11 libawt_headless/xawt set,
# not built).  Font's clinit needs it before ANY Graphics2D use; the real one
# only caches field IDs for the X11 font metrics natives.  No-op for now --
# the real font pipeline (freetype/fontmanager) is the M2 work item.
cat > "$OUT/libawt/font_initids_compat.c" <<'FEOF'
#include "jni.h"
JNIEXPORT void JNICALL
Java_java_awt_Font_initIDs(JNIEnv *env, jclass cls) { }
FEOF
$CC $AWTINC -c "$OUT/libawt/font_initids_compat.c" -o "$OUT/libawt/font_initids_compat.o" 2>"$OUT/e" \
    && echo "  Font.initIDs compat OK" || { echo "  Font.initIDs compat FAIL"; head -3 "$OUT/e"; }

echo "=== link libawt.so ==="
if ppc-amigaos-gcc -mcrt=clib4 -fPIC -shared -Wl,-rpath=SYS:Test \
       -o "$OUT/libawt.so" "$OUT"/libawt/*.o 2>"$OUT/e"; then
    echo "  libawt.so OK ($(wc -c < "$OUT/libawt.so") bytes)"
else
    echo "  libawt.so LINK FAIL"; head -10 "$OUT/e"
fi

echo "=== libfontmanager.so (freetype rasterizer; M2 fonts) ==="
# C subset of share/native/sun/font: the freetype scaler + font/glyph natives.
# The ICU layout engine (layout/*.cpp, C++ -- complex-script shaping only) is
# NOT built; its sun.font.SunLayoutEngine natives surface only via TextLayout
# of complex scripts.  freetype: SDK/local clib4 static libfreetype.a.
FT_INC="-I /opt/ppc-amigaos/ppc-amigaos/SDK/local/common/include \
 -I /opt/ppc-amigaos/ppc-amigaos/SDK/local/common/include/freetype2"
FT_LIB="-L /opt/ppc-amigaos/ppc-amigaos/SDK/local/clib4/lib"
FONTFILES="sunFont.c freetypeScaler.c DrawGlyphList.c AccelGlyphCache.c"
FONTSRC="$SH/font"
FMINC="-I $HDR -I $COMPAT $FT_INC \
 -I $J/src/share/javavm/export -I $J/src/solaris/javavm/export \
 -I $J/src/share/native/common -I $J/src/solaris/native/common \
 -I $FONTSRC -I $SH/awt/image/cvutils -I $SH/java2d -I $SOL/java2d \
 -I $SH/java2d/loops -I $SH/java2d/pipe -I $SH/awt/debug -I $SH/awt \
 -include $COMPAT/jdkdefs.h"

FCLASSES=$(cat $(for f in $FONTFILES; do echo "$FONTSRC/$f"; done) "$FONTSRC"/*.h 2>/dev/null \
    | grep -ohE '"(java|sun)_[A-Za-z0-9_]+\.h"' \
    | sed 's/"//g; s/\.h$//; s/_/./g' | sort -u)
for c in $FCLASSES; do
    /opt/jdk8/bin/javah -d "$HDR" -classpath "$RTJAR" "$c" >/dev/null 2>&1 || true
done

mkdir -p "$OUT/libfontmanager"
fok=0; ffail=0
for f in $FONTFILES; do
    if $CC $FMINC -c "$FONTSRC/$f" -o "$OUT/libfontmanager/$(basename "$f" .c).o" 2>"$OUT/e"; then
        fok=$((fok+1))
    else
        ffail=$((ffail+1)); echo "  FONT FAIL $f"
        grep -m2 -E "error:|No such file" "$OUT/e" | sed 's/^/        /'
    fi
done
# fontconfig bridge stubs: no libfontconfig on AmigaOS.  getFontConfig leaves
# FontConfigInfo unfilled -> FcFontConfiguration.init() fails -> the font manager
# falls back to MFontConfiguration + java.home/lib/fontconfig.properties, and
# fonts are registered from java.home/lib/fonts.  (fontpath.c is X11-heavy and
# not built.)
cat > "$OUT/libfontmanager/fontconfig_stub.c" <<'FCEOF'
#include "jni.h"
JNIEXPORT jint JNICALL
Java_sun_font_FontConfigManager_getFontConfigVersion(JNIEnv *env, jclass cls)
{ return 0; }
JNIEXPORT void JNICALL
Java_sun_font_FontConfigManager_getFontConfig(JNIEnv *env, jclass cls,
    jstring locale, jobject fcInfo, jobjectArray fcCompFonts, jboolean prefLocale)
{ /* leave fcInfo unfilled: Fc config init fails -> properties fallback */ }
JNIEXPORT jint JNICALL
Java_sun_font_FontConfigManager_getFontConfigAASettings(JNIEnv *env, jclass cls,
    jstring locale, jstring fcFamily)
{ return -1; }
JNIEXPORT jstring JNICALL
Java_sun_awt_FcFontManager_getFontPathNative(JNIEnv *env, jobject thiz,
    jboolean noType1, jboolean isX11)
{ return (*env)->NewStringUTF(env, ""); }
FCEOF
if $CC $FMINC -c "$OUT/libfontmanager/fontconfig_stub.c" -o "$OUT/libfontmanager/fontconfig_stub.o" 2>"$OUT/e"; then
    fok=$((fok+1)); echo "  fontconfig stubs OK"
else
    echo "  fontconfig stubs FAIL"; head -4 "$OUT/e"
fi

echo "  libfontmanager compile: $fok OK, $ffail FAILED"
if ppc-amigaos-gcc -mcrt=clib4 -fPIC -shared -Wl,-rpath=SYS:Test \
       -o "$OUT/libfontmanager.so" "$OUT"/libfontmanager/*.o $FT_LIB -lfreetype 2>"$OUT/e"; then
    echo "  libfontmanager.so OK ($(wc -c < "$OUT/libfontmanager.so") bytes)"
else
    echo "  libfontmanager.so LINK FAIL"; head -10 "$OUT/e"
fi
echo "  undefined check:"; ppc-amigaos-nm -D -u "$OUT/libfontmanager.so" 2>/dev/null \
    | grep -vE "Jam_|JVM_|JNU_|JNI_|jio_|jvm|__|mem|str|malloc|free|calloc|realloc|printf|sprintf|fprintf|qsort|getenv|sqrt|floor|ceil|pow|abs|fopen|fclose|fread|fseek|ftell|fflush|sscanf|longjmp|setjmp" | head -8
