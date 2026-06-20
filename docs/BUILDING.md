# Building Java-OS4

Everything cross-compiles inside a Docker image that carries the AmigaOS 4
PowerPC toolchain and a host JDK 8. You do not need an Amiga to build — only to
run the result.

## Prerequisites

- **Docker.** The build image (`tools/Dockerfile`) derives from
  [`walkero/amigagccondocker:os4-gcc11`](https://hub.docker.com/r/walkero/amigagccondocker)
  — the public AmigaOS 4 cross-toolchain image (`ppc-amigaos-gcc` 11.5.0 + SDK +
  clib4) — and adds a host JDK 8 + `ecj`. The base is pulled automatically the
  first time you build the image.
- **The clib4 C runtime** — vendored as the **`clib4/` git submodule**
  (`AmigaLabs/clib4`, `development` branch). Check it out with
  `git submodule update --init` (or clone the repo with `--recursive`). The build
  compiles clib4 from this submodule, so the runtime always matches the build.
- **The vendored JamVM + OpenJDK 8 sources** — large public upstream trees,
  gitignored here (only our changes are tracked, as the patch under `docs/`).
  Fetch them once with **`make vendor`** (or `sh tools/fetch-vendor.sh`): it clones
  [`jaokim/jamiga-jamvm`](https://github.com/jaokim/jamiga-jamvm) into
  `vendor/jamvm` and applies `docs/jamvm-amiga-openjdk.patch`, and clones
  [`jaokim/jamiga-icedtea8-3.0`](https://github.com/jaokim/jamiga-icedtea8-3.0)
  into `vendor/icedtea8` (see *Getting the OpenJDK 8 native sources* below).

Build the image once (rebuild when `tools/Dockerfile` changes):

```sh
docker build -t javaos4-build:latest -f tools/Dockerfile .
```

> **Windows:** run Docker from PowerShell (call `docker.exe` directly). The
> Git-Bash/MSYS layer rewrites the `-v`/`-w` paths and breaks the mounts.

In the commands below the working directory is the repository root.

## Build steps

The `Makefile` drives the cross build (each target runs the matching `tools/`
script inside the image and writes to `build/`):

```sh
git submodule update --init       # check out the clib4/ submodule, once
make vendor                       # fetch JamVM + IcedTea 8 upstream sources, once
make image                        # build the build image (pulls walkero base), once
make build                        # clib4 + VM + OpenJDK/AWT natives + toolkit
make dist                         # gather build/ -> build/release/Java/ + .lha
make release                      # build then dist, in one step
```

`make build` expands to, in order: `build-jamvm-openjdk.sh` (the VM:
`libjvm.so` + the `jamvm-openjdk` launcher), `build-openjdk-natives.sh`
(`libjava`/`libzip`/`libnio`/`libnet`/…), `build-awt-natives.sh` +
`build-amigaawt.sh` (`libawt`/`libfontmanager` + the Amiga windowing JNI), and
`build-amigatoolkit.sh` (the `sun.awt.amiga` toolkit + test zips). `make dist`
runs `package.sh`, which only gathers existing `build/` outputs — it compiles
nothing and needs no clib4 mount. You can still invoke any `tools/` script by
hand inside the image if you prefer.

The class-library jars (`rt.jar`, `charsets.jar`, …) come from the Temurin 8 JDK
in the build image; `package.sh` gathers them along with the VM, the native
libraries, the clib4/support shared objects, the font data, and the launcher.

### Getting the OpenJDK 8 native sources

`make natives` compiles `libjava` / `libawt` / `libfontmanager` / … from the
OpenJDK 8 C sources, which are not committed here. The IcedTea 8 harness in
`vendor/icedtea8` (fetched by `make vendor`) downloads and extracts OpenJDK 8u
for you — build it per [`vendor/icedtea8/README`](../vendor/icedtea8/README) (a
standard `./configure && make`); it writes the OpenJDK 8u source tree under
`build/openjdk8/`. The native build scripts expect that tree at
`build/openjdk8/jdk-<changeset>`, so set `J=` near the top of
`tools/build-openjdk-natives.sh` and `tools/build-awt-natives.sh` to match the
directory the harness creates. (`make vm` and `make clib4` do **not** need this
step — only the OpenJDK native libraries do.)

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
