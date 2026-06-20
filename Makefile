# Java-OS4 -- top-level build orchestration.
#
# All real work happens in the cross-build Docker image; these targets just
# invoke the scripts in tools/ inside it.  See docs/BUILDING.md.
#
#   make vendor    fetch the vendored JamVM + IcedTea 8 upstream sources (once)
#   make image     build the cross-build Docker image (once)
#   make clib4     build the clib4 C runtime from the in-repo clib4/ submodule
#   make vm        build the VM (libjvm.so + jamvm-openjdk launcher)
#   make natives   build the OpenJDK 8 + AWT native libraries
#   make toolkit   build the sun.awt.amiga toolkit + test zips
#   make tests     build the VM test suite + bundled examples
#   make build     clib4 + vm + natives + toolkit + tests
#   make dist      assemble the install tree + the .lha release  (alias: lha)
#   make release   build everything, then dist
#   make clean-dist  remove build/release and the .lha
#
# First-time setup (see docs/BUILDING.md): `make vendor` fetches the large public
# upstream sources (jaokim's JamVM + IcedTea 8) that are gitignored here, and the
# clib4 C runtime is the in-repo clib4/ git submodule (AmigaLabs/clib4,
# `development`) that `make build` checks out automatically.  No external paths.
#
# Windows: run from PowerShell (the Git-Bash MSYS layer mangles docker paths).

include config.mk

VERSION := $(shell cat VERSION)
LHA     := build/JavaOS4-$(VERSION).lha

# Run a tools/ script in the build image.  The whole project tree -- including
# the clib4/ submodule -- is mounted at /work, so the build scripts read clib4
# from /work/clib4 (no separate mount).  The MSYS_* vars stop Git-Bash/MSYS2 on
# Windows from rewriting the container paths; they are ignored on Linux/macOS.
DOCKER = MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL='*' docker
RUN    = $(DOCKER) run --rm -v "$(CURDIR):/work" -w /work $(DOCKER_IMAGE) sh

.PHONY: vendor image clib4 vm natives toolkit tests build dist lha release clean-dist help

help:
	@sed -n 's/^#   //p' Makefile

# Fetch the vendored JamVM + IcedTea 8 upstream sources (clone + apply our patch).
vendor:
	sh tools/fetch-vendor.sh

image:
	docker build -t $(DOCKER_IMAGE) -f tools/Dockerfile .

# Check out the clib4 submodule on a fresh clone (no-op once present).
clib4/GNUmakefile.os4:
	git submodule update --init clib4

# Build clib4.library + the .so/.a front-ends from the submodule -> clib4/build.
clib4: clib4/GNUmakefile.os4
	$(RUN) /work/tools/build-clib4.sh

vm: clib4
	$(RUN) /work/tools/build-jamvm-openjdk.sh

natives: clib4
	$(RUN) /work/tools/build-openjdk-natives.sh
	$(RUN) /work/tools/build-awt-natives.sh
	$(RUN) /work/tools/build-amigaawt.sh

toolkit:
	$(RUN) /work/tools/build-amigatoolkit.sh

tests:
	$(RUN) /work/tools/build-tests.sh

build: clib4 vm natives toolkit tests

# Gather the build outputs into build/release/Java and pack the .lha.
# Run after `make build` (package.sh compiles nothing -- it only collects).
dist:
	$(RUN) /work/tools/package.sh
	@echo "Release: $(LHA)"

lha: dist

release: build dist

clean-dist:
	rm -rf build/release $(LHA)
