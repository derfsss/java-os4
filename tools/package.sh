#!/bin/sh
# Phase 5: assemble the Java-OS4 release -- an Installation Utility package.
#
# Runs in the javaos4-build container (has /opt/jdk8, the DejaVu fonts, and lha).
# Needs only the repo mounted at /work, with build/ already populated by the
# build scripts (it gathers their outputs -- it does not compile anything).
#
# Produces the distribution drawer + its icon and a .lha of both:
#   build/release/Java-OS4/            installer drawer (double-click to install)
#     install.py                       AmigaOS 4.1 Installation Utility script
#     JavaOS4InstallerLocale.py        locale strings
#     install.py.info                  project icon -> Sys:Utilities/Installation Utility
#     content/Java/                    the runtime payload copied to the chosen drawer
#   build/release/Java-OS4.info        distribution-drawer icon (beside the drawer)
#   build/JavaOS4-<ver>.lha            the release archive
#
# The runtime layout is FLAT (the boot classpath is colon-free + CWD-relative
# because JamVM's C boot-path parser splits on ':'; see docs), so the runtime
# lives in one drawer and the `java` launcher CDs into JAVA: (the assign the
# installer adds to S:User-Startup).
set -e

VER=$(cat /work/VERSION 2>/dev/null || echo "0.0.0")
PVER=$(echo "$VER" | cut -d. -f1,2)          # 0.5.0 -> 0.5 (clean major.minor)
DATE=$(date +%d.%m.%Y)
# Official Java version the runtime is built on (the class-library JDK).
JVER=$(sed -n 's/^JAVA_VERSION="\(.*\)"$/\1/p' /opt/jdk8/release 2>/dev/null)
[ -n "$JVER" ] || JVER="1.8.0"
JDK8=/opt/jdk8
B=/work/build
N="$B/openjdk-natives"
SRC=/work/src/installer
OUT="$B/release"
R="$OUT/Java-OS4"          # distribution drawer
RT="$R/content/Java"       # runtime payload (installed into the chosen drawer)

echo "=== Java-OS4 $VER -- assembling $R ==="
rm -rf "$OUT"
mkdir -p "$RT/lib/fonts"

# --- runtime: VM + launcher -----------------------------------------------
cp "$B/jamvm-openjdk" "$RT/jamvm-openjdk"
cp "$B/libjvm.so"     "$RT/"

# `java` launcher: CD into JAVA: (assigned to the install drawer by install.py)
# so the colon-free, CWD-relative default boot classpath resolves, and point
# the loader at the bundled sobjs (the .so rpath is the dev build dir;
# LD_LIBRARY_PATH="." overrides to the install drawer).
{
    echo ".KEY args/F"
    echo ".BRA {"
    echo ".KET }"
    echo ";\$VER: Java-OS4 $PVER ($DATE) OpenJDK $JVER"
    echo "CD JAVA:"
    echo 'SetEnv LD_LIBRARY_PATH "."'
    echo "JAVA:jamvm-openjdk {args}"
} > "$RT/java"

# --- runtime: OpenJDK + AWT natives ---------------------------------------
for so in libjava libverify libzip libnio libnet \
          libawt libfontmanager libamigaawt; do
    cp "$N/$so.so" "$RT/"
done

# --- runtime: CRT / support shared objects --------------------------------
cp "$B/sobjs/"libc.so "$B/sobjs/"libpthread.so "$B/sobjs/"libm.so \
   "$B/sobjs/"librt.so "$B/sobjs/"libz.so.1 "$B/sobjs/"libgcc.so "$RT/"

# --- runtime: clib4.library (the C runtime the VM + .so stubs call into) ---
# The bundled .so stubs (libc.so, ...) are clib4.library front-ends; the real
# C runtime lives in clib4.library, which must be present in LIBS: at runtime.
# Ship it so the installer can put it there on a machine that has no clib4 --
# the .so stubs alone are not enough (this was the missing-requirement that
# blocked installs in 0.5.0).  This is the exact build the VM was validated
# against (incl. the AltiVec vec_strcpy page-overread fix); keep it in lockstep
# with build/sobjs/.  clib4 2.1+.
cp /work/build/clib4.library "$RT/"

# --- runtime: class library + toolkit -------------------------------------
cp "$B/jars/"rt.jar "$B/jars/"charsets.jar "$B/jars/"jce.jar \
   "$B/jars/"jsse.jar "$B/jars/"resources.jar "$RT/"
cp "$N/niopatch.zip"     "$RT/"
cp "$B/amigatoolkit.zip" "$RT/"

# --- examples + test suite (runnable out of the box) ----------------------
# 0.5.0 shipped nothing to run but `java -version`; bundle a headless demo, a
# Swing demo, and the self-verifying VM test suite.
mkdir -p "$RT/examples"
cp "$B/examples/"HelloJava.jar "$B/examples/"SwingDemo.jar "$RT/examples/"
cp "$B/testsuite.zip" "$RT/examples/"

# --- runtime: lib/ resources (read from java.home/lib) --------------------
cp /work/src/fontconfig/fontconfig.properties "$RT/lib/"
cp /usr/share/fonts/truetype/dejavu/DejaVuSans.ttf \
   /usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf "$RT/lib/fonts/"
for p in currency.data tzdb.dat calendars.properties content-types.properties \
         flavormap.properties hijrah-config-umalqura.properties \
         logging.properties net.properties psfontj2d.properties \
         sound.properties; do
    cp "$JDK8/jre/lib/$p" "$RT/lib/" 2>/dev/null || true
done

# --- runtime: version + README --------------------------------------------
echo "$VER" > "$RT/VERSION"
cat > "$RT/README" <<README
Java-OS4 $VER -- runtime
========================

A Java 8 runtime for AmigaOS 4: JamVM 2.0 + the OpenJDK 8 class library, with a
Swing/AWT toolkit so Java GUIs run in Workbench windows.

Requirements
  clib4.library 2.1 or newer in LIBS: -- the C runtime the VM depends on.  The
  installer copies the bundled clib4.library there if it is missing.

The installer assigns JAVA: to this drawer (added to S:User-Startup so it
survives reboots) and copies the 'java' launcher to C: so it runs from any
Shell.  Try:

    java -version
    java -cp examples/HelloJava.jar HelloJava
    java -cp examples/SwingDemo.jar  SwingDemo
    java -cp examples/testsuite.zip  VmSuite

Swing/AWT applications need no extra flags -- the Amiga toolkit is the default.
App classpath entries resolve from the JAVA: drawer (the launcher CDs there);
reference jars elsewhere by absolute path.  'javac' is not included -- compile
on a host JDK 8 with 'javac --release 8' and copy the jar over.  Bytecode newer
than Java 8 is rejected up front with UnsupportedClassVersionError.

Licensing: JamVM GPLv2; OpenJDK 8 GPLv2 + Classpath exception.
README

# --- installer (Installation Utility / Python 2.5) ------------------------
sed -e "s/@VERSION@/$VER/g" -e "s/@DATE@/$DATE/g" \
    "$SRC/install.py" > "$R/install.py"
cp "$SRC/JavaOS4InstallerLocale.py" "$R/"
cp "$SRC/install.py.info"           "$R/install.py.info"
cp "$SRC/drawer.info"               "$OUT/Java-OS4.info"
# drawer icon the installer copies to "<dest>.info" so the install drawer
# gets a Workbench icon.
cp "$SRC/drawer.info"               "$R/content/drawer.info"

# --- archive (the drawer + its icon) --------------------------------------
rm -f "$B/JavaOS4-$VER.lha"        # lha 'a' appends; start clean
( cd "$OUT" && lha -aq2 "$B/JavaOS4-$VER.lha" Java-OS4 Java-OS4.info >/dev/null )
echo "=== distribution tree ==="
( cd "$OUT" && find Java-OS4 Java-OS4.info -type f | sort | sed 's,^,  ,' )
echo "=== JavaOS4-$VER.lha ($(wc -c < "$B/JavaOS4-$VER.lha") bytes) ==="
lha -l "$B/JavaOS4-$VER.lha" | tail -6
