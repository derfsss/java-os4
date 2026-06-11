# Building Java-OS4

Everything cross-compiles inside a Docker image that carries the AmigaOS 4
PowerPC toolchain and a host JDK 8. You do not need an Amiga to build — only to
run the result.

## Prerequisites

- **Docker.**
- **The base cross-toolchain image** `amigaos4-gcc11:latest` — the
  `ppc-amigaos-gcc` 11.5.0 toolchain with the AmigaOS 4 SDK and clib4. The build
  image (`tools/Dockerfile`) derives from it and adds a host JDK 8 + `ecj`.
- **A clib4 checkout** (the C runtime), mounted into the container at `/clib4`.
  The runtime must match the version you deploy to the target.

Build the image once (rebuild when `tools/Dockerfile` changes):

```sh
docker build -t javaos4-build:latest -f tools/Dockerfile .
```

> **Windows:** run Docker from PowerShell (call `docker.exe` directly). The
> Git-Bash/MSYS layer rewrites the `-v`/`-w` paths and breaks the mounts.

In the commands below, `$CLIB4` is the path to your clib4 checkout, and the
working directory is the repository root.

## Build steps

Each script runs inside the image and writes to `build/`:

```sh
DR="docker run --rm -v $PWD:/work -v $CLIB4:/clib4 javaos4-build:latest sh"

# 1. The VM: libjvm.so (shared) + the jamvm-openjdk launcher.
$DR /work/tools/build-jamvm-openjdk.sh

# 2. OpenJDK 8 core native libraries (libjava, libzip, libnio, libnet, ...).
$DR /work/tools/build-openjdk-natives.sh

# 3. AWT/Java2D natives (libawt, libfontmanager) + the Amiga windowing JNI.
$DR /work/tools/build-awt-natives.sh
$DR /work/tools/build-amigaawt.sh

# 4. The sun.awt.amiga toolkit + test apps -> amigatoolkit.zip / swingtest.zip.
$DR /work/tools/build-amigatoolkit.sh

# 5. Assemble the install tree and the release archive.
$DR /work/tools/package.sh        # -> build/release/Java/ and build/JavaOS4-<ver>.lha
```

The class-library jars (`rt.jar`, `charsets.jar`, …) come from the Temurin 8 JDK
in the build image; `package.sh` gathers them along with the VM, the native
libraries, the clib4/support shared objects, the font data, and the launcher.

## The VM as a shared library

On AmigaOS 4 an executable does not export symbols to `dlopen`'d objects, so the
VM is built as **`libjvm.so`** (exporting `JVM_*` / `JNI_*`) and the `java`
launcher links against it. The OpenJDK native libraries resolve their VM symbols
against `libjvm.so` at load time, and all components share one clib4 instance via
clib4's shared-library model (`-use-dynld` launcher + plain `-shared` natives,
with the clib4 `.so` objects shipped alongside).

## Running

A built release is a self-contained `Java/` drawer (see the top-level
[README](../README.md) for end-user install/run). A few things matter when
deploying or testing:

- **Run via a real CLI process.** clib4's `pthread_create` needs a proper
  process context (`CreateNewProc`), so launch through `Run`/`Execute`, e.g.
  `Run Execute <script>`, rather than a non-interactive remote exec.
- **The boot classpath is relative.** JamVM's boot-classpath parser splits on
  `:`, which collides with AmigaOS `Volume:` names, so the default boot classpath
  is colon-free and resolved against the current directory. The `java` launcher
  therefore `CD`s into the `Java:` drawer before starting the VM.
- **Shared objects.** The launcher sets `LD_LIBRARY_PATH` so the bundled clib4
  and support `.so` files load from the install drawer. Ship these with the app;
  do **not** overwrite the system `SOBJS:`.
- **Execute bit.** After copying a binary onto the target, ensure its `e`
  (execute) protection bit is set (`Protect <file> +e`) — without it AmigaDOS
  silently refuses to run it.

### Testing on QEMU

The fast development loop uses an AmigaOS 4 PowerPC QEMU machine
(`qemu-system-ppc -M amigaone`, with `graphics.library`-capable display).
Transfer the install drawer to the guest, assign `Java:` to it, and run the
launcher. The `tests/` directory holds self-verifying programs (conformance
suites and GUI tests) that print `[PASS]`/`[FAIL]` lines you can capture from a
redirected output file.

### Testing on hardware

The primary bring-up target is an AmigaOne X5000. Copy the `Java/` drawer over,
assign `Java:`, and run as above.

## Versioning

The project version is in the top-level `VERSION` file and stamped into the
release archive name. Releases are tagged `vMAJOR.MINOR.PATCH`.
