#!/bin/sh
# Phase 5: assemble a relocatable Java-OS4 install tree and a .lha release.
#
# Runs in the javaos4-build container (has /opt/jdk8, the DejaVu fonts, and lha).
# Needs only the repo mounted at /work, with build/ already populated by the
# build scripts (it gathers their outputs -- it does not compile anything).
# Produces:
#   /work/build/release/Java/        -- the install tree (-> assign Java:)
#   /work/build/JavaOS4-<ver>.lha    -- the release archive
#
# The runtime layout is FLAT (the boot classpath is colon-free + CWD-relative
# because JamVM's C boot-path parser splits on ':'; see docs), so everything
# lives in the one install dir and the `java` launcher CDs into it.
set -e

VER=$(cat /work/VERSION 2>/dev/null || echo "0.0.0")
JDK8=/opt/jdk8
B=/work/build
N="$B/openjdk-natives"
OUT="$B/release"
R="$OUT/Java"

echo "=== Java-OS4 $VER -- assembling $R ==="
rm -rf "$OUT"
mkdir -p "$R/lib/fonts"

# --- VM + launcher --------------------------------------------------------
cp "$B/jamvm-openjdk" "$R/jamvm-openjdk"
cp "$B/libjvm.so"     "$R/"

# `java` launcher: CD into the install dir so the colon-free, CWD-relative
# default boot classpath resolves, and point the loader at the local sobjs
# (the .so rpath is the dev SYS:Test; LD_LIBRARY_PATH="." overrides to here).
cat > "$R/java" <<'LAUNCH'
.KEY args/F
.BRA {
.KET }
CD Java:
SetEnv LD_LIBRARY_PATH "."
Java:jamvm-openjdk {args}
LAUNCH

# --- OpenJDK + AWT natives ------------------------------------------------
for so in libjava libverify libzip libnio libnet \
          libawt libfontmanager libamigaawt; do
    cp "$N/$so.so" "$R/"
done

# --- CRT / support shared objects -----------------------------------------
cp "$B/sobjs/"libc.so "$B/sobjs/"libpthread.so "$B/sobjs/"libm.so \
   "$B/sobjs/"librt.so "$B/sobjs/"libz.so.1 "$B/sobjs/"libgcc.so "$R/"

# --- class library + toolkit ----------------------------------------------
cp "$B/jars/"rt.jar "$B/jars/"charsets.jar "$B/jars/"jce.jar \
   "$B/jars/"jsse.jar "$B/jars/"resources.jar "$R/"
cp "$N/niopatch.zip"     "$R/"
cp "$B/amigatoolkit.zip" "$R/"

# --- lib/ resources (fonts + data the runtime reads from java.home/lib) ----
cp /work/src/fontconfig/fontconfig.properties "$R/lib/"
cp /usr/share/fonts/truetype/dejavu/DejaVuSans.ttf \
   /usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf "$R/lib/fonts/"
for p in currency.data tzdb.dat calendars.properties content-types.properties \
         flavormap.properties hijrah-config-umalqura.properties \
         logging.properties net.properties psfontj2d.properties \
         sound.properties; do
    cp "$JDK8/jre/lib/$p" "$R/lib/" 2>/dev/null || true
done

# --- version, README, installer -------------------------------------------
echo "$VER" > "$R/VERSION"

cat > "$R/README" <<README
Java-OS4 $VER
=============

A Java 8 runtime for AmigaOS 4 (PowerPC): JamVM 2.0 + the OpenJDK 8 class
library on clib4, with an Intuition/graphics.library AWT toolkit so Swing
runs in Workbench windows.

INSTALL
  Double-click "Install", or from a Shell:  Execute Install
  This adds the assign  Java:  pointing at this drawer.  To make it
  permanent, copy the AddAssign line into S:User-Startup.

RUN
  Java: must be assigned (the Install script does this).
    java -version
    java -cp myapp.jar Main          ; jar in Java: (or use an absolute path)
  GUI (Swing) apps need no extra flags -- the Amiga toolkit is the default.

NOTES
  - App classpath entries are resolved from the Java: drawer (the launcher
    CDs there); reference app jars by absolute path to run them from elsewhere.
  - 'javac' is not included -- compile on a host JDK 8 and copy the jar over.

Licensing: JamVM GPLv2; OpenJDK 8 GPLv2 + Classpath exception.
README

# AmigaDOS install script: assign Java: to THIS drawer.  Run it from within
# the drawer (Execute Install, or double-click).  Uses the standard idiom of
# capturing the current directory via `Cd` (which prints it with no arg).
cat > "$R/Install" <<'INSTALL'
.KEY noop
; Java-OS4 installer -- assign Java: to the current drawer.
Cd >ENV:JavaOS4Dir ""
Assign Java: "$JavaOS4Dir"
Delete >NIL: ENV:JavaOS4Dir
Echo "Java: assigned to this drawer for the current session."
Echo "To make it permanent, add an 'Assign Java: <this path>' line to"
Echo "S:User-Startup."
INSTALL

# --- archive --------------------------------------------------------------
( cd "$OUT" && lha -aq2 "$B/JavaOS4-$VER.lha" Java >/dev/null )
echo "=== install tree ==="
( cd "$OUT" && find Java -type f | sort | sed 's,^,  ,' )
echo "=== JavaOS4-$VER.lha ($(wc -c < "$B/JavaOS4-$VER.lha") bytes) ==="
lha -l "$B/JavaOS4-$VER.lha" | tail -8
