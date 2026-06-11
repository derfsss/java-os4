# Java-OS4 -- top-level build orchestration.
#
# All real work happens in the cross-build Docker image; these targets just
# invoke the scripts in tools/ inside it.  See docs/BUILDING.md.
#
#   make image     build the cross-build Docker image (once)
#   make vm        build the VM (libjvm.so + jamvm-openjdk launcher)
#   make natives   build the OpenJDK 8 + AWT native libraries
#   make toolkit   build the sun.awt.amiga toolkit + test zips
#   make build     vm + natives + toolkit
#   make dist      assemble the install tree + the .lha release  (alias: lha)
#   make release   build everything, then dist
#   make clean-dist  remove build/release and the .lha
#
# CLIB4 must point at a clib4 checkout for the build targets (not needed for
# dist):  make build CLIB4=/path/to/clib4
#
# Windows: run from PowerShell (the Git-Bash MSYS layer mangles docker paths).

include config.mk

VERSION := $(shell cat VERSION)
LHA     := build/JavaOS4-$(VERSION).lha

CLIB4 ?= ../clib4

# Run a tools/ script in the build image.  *_C also mounts the clib4 checkout.
# The MSYS_* vars stop Git-Bash/MSYS2 on Windows from rewriting the container
# paths (e.g. "/work" -> "C:/Program Files/Git/work"); they are ignored on
# Linux/macOS.
DOCKER = MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL='*' docker
RUN    = $(DOCKER) run --rm -v "$(CURDIR):/work" -w /work $(DOCKER_IMAGE) sh
RUN_C  = $(DOCKER) run --rm -v "$(CURDIR):/work" -v "$(CLIB4):/clib4" -w /work \
            $(DOCKER_IMAGE) sh

.PHONY: image vm natives toolkit build dist lha release clean-dist help

help:
	@sed -n 's/^#   //p' Makefile

image:
	docker build -t $(DOCKER_IMAGE) -f tools/Dockerfile .

vm:
	$(RUN_C) /work/tools/build-jamvm-openjdk.sh

natives:
	$(RUN_C) /work/tools/build-openjdk-natives.sh
	$(RUN_C) /work/tools/build-awt-natives.sh
	$(RUN_C) /work/tools/build-amigaawt.sh

toolkit:
	$(RUN_C) /work/tools/build-amigatoolkit.sh

build: vm natives toolkit

# Gather the build outputs into build/release/Java and pack the .lha.
# Run after `make build` (package.sh compiles nothing -- it only collects).
dist:
	$(RUN) /work/tools/package.sh
	@echo "Release: $(LHA)"

lha: dist

release: build dist

clean-dist:
	rm -rf build/release $(LHA)
