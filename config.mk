# Java-OS4 -- shared build configuration.
# Included by sub-Makefiles / scripts to avoid duplicating Docker and compiler
# settings.

# Base image: walkero/amigagccondocker:os4-gcc11 -- the public AmigaOS 4 cross
# toolchain (SDK + clib4 + autotools, no host Java) on Docker Hub:
# https://hub.docker.com/r/walkero/amigagccondocker
DOCKER_BASE_IMAGE = walkero/amigagccondocker:os4-gcc11
# Derived image (adds host JDK + ecj for GNU Classpath); built from tools/Dockerfile.
DOCKER_IMAGE      = javaos4-build:latest

# If the cross toolchain is already on PATH (i.e. we're inside the container),
# DOCKER_RUN is a no-op; otherwise wrap the command in a throwaway container.
# Windows note: build from PowerShell via build.ps1 (docker.exe directly) -- the
# Git-Bash MSYS layer mangles the `-w /work` argv for docker.exe.
ifeq ($(shell command -v ppc-amigaos-gcc 2>/dev/null),)
DOCKER_RUN = docker run --rm -v "$(shell pwd):/work" -w /work $(DOCKER_IMAGE)
else
DOCKER_RUN =
endif

# Cross toolchain.
CROSS_HOST = ppc-amigaos
CC         = ppc-amigaos-gcc
CXX        = ppc-amigaos-g++
AR         = ppc-amigaos-ar
RANLIB     = ppc-amigaos-ranlib
STRIP      = ppc-amigaos-strip

# Everything we ship lives in the clib4 (POSIX) zone: CACAO, Classpath natives,
# and the AWT JNI bridge.  pthreads/mmap(PROT_EXEC)/dlopen/sockets/C++EH come
# from clib4.  Do NOT add -D__AMIGAOS4__ or -I for the SDK (predefined / default).
CLIB4_CFLAGS   = -mcrt=clib4 -O2 -Wall
CLIB4_CXXFLAGS = -mcrt=clib4 -O2 -std=c++17 -Wall
CLIB4_LDFLAGS  = -mcrt=clib4
CLIB4_LIBS     = -lpthread -lm -lauto -latomic

# SDK paths inside the container.
SDK_BASE   = /opt/ppc-amigaos/ppc-amigaos/SDK
SDK_INC    = $(SDK_BASE)/include/include_h
SDK_CLIB4  = $(SDK_BASE)/clib4
