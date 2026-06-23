# Changelog

All notable changes to Java-OS4. Versions are AmigaOS 4 `.lha` releases on the
[Releases page](https://github.com/derfsss/java-os4/releases). Java-OS4 is a Java 8
runtime for AmigaOS 4 (PowerPC): JamVM 2.0 + the OpenJDK 8 class library + a native
`sun.awt.amiga` AWT/Swing toolkit.

## 0.5.4 — 2026-06-23

**Fixed**

- **`sun.boot.class.path` separator** — the property was exposed as the VM's raw
  `:`-joined boot path (`niopatch.zip:resources.jar:rt.jar:…`), but on AmigaOS
  `path.separator` is `;` (`:` is the volume separator). OpenJDK's
  `sun.misc.Launcher.getBootstrapClassPath()` splits the property on `path.separator`,
  so the whole value was read as one bogus entry beginning `niopatch.zip:` →
  AmigaDOS popped a **"Please insert volume niopatch.zip"** requester and every
  `getBootstrapResource()` failed. That broke `sun.reflect.misc.MethodUtil`, which
  then defined `sun.reflect.misc.Trampoline` via the bootstrap loader and threw
  **"Trampoline must not be defined by the bootstrap class loader"** — first hit
  running Swing applications and the test suite (amigans.net report). The property
  is now emitted as a `;`-separated list of absolute paths anchored at `java.home`,
  so it splits correctly and every boot resource resolves. (`HelloJava` was never
  affected.) Headless apps, `java -version`, and the regression suite are unchanged.

**Added**

- `tests/regression/BootClassPathTest.java` — verifies the boot-classpath property
  splits correctly, a bootstrap resource resolves, and a `java.beans.Expression`
  reflective bounce through `MethodUtil`/`Trampoline` succeeds (fails on the
  pre-0.5.4 VM, passes on 0.5.4 and on a reference JVM).

## 0.5.3 — Amiga-1251 charset

- Fixed a `NoClassDefFoundError` for the Amiga-1251 charset at VM bootstrap.

## 0.5.2 — `java -cp`

- Fixed `java -cp` for both relative and absolute `Volume:dir` classpath entries,
  resolved against the caller's directory with no shell-cwd leak (`path.separator`
  set to `;`; each `-cp` entry rewritten to an absolute Unix-form path).
- Bundled zlib 1.2.13 in `libzip.so` (was 1.2.8).

## 0.5.1 — hardware + packaging

- Validated installing and running on real AmigaOne X5000 hardware.
- Installer/packaging fixes; the GitHub Releases page is the download source for
  prebuilt `.lha` archives.

## 0.5.0 — first release

- First packaged release: an Installation-Utility `.lha` assembling the VM
  (`jamvm-openjdk` + `libjvm.so`), the OpenJDK 8 class library, the OpenJDK + AWT
  native libraries, the `sun.awt.amiga` toolkit, the clib4 runtime, fonts and
  resources, a `java` launcher, and runnable examples. Runs headless Java 8
  programs and Swing applications in Workbench windows.
