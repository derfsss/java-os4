# Java-OS4 build image.
#
# Base provides the AmigaOS 4 cross toolchain (ppc-amigaos-gcc 11.5.0), the SDK,
# clib4, and autotools.  It does NOT ship a host Java compiler, which we need to
# compile GNU Classpath's Java sources during the cross build.  This derived
# image adds a host JDK + ecj (Eclipse batch compiler) and a few build helpers.
#
# Build once (from project root):  make image   (or: docker build -t javaos4-build:latest -f tools/Dockerfile .)
# Rebuild whenever this file changes.
FROM amigaos4-gcc11:latest

RUN apt-get update && apt-get install -y --no-install-recommends \
        default-jdk-headless \
        ecj \
        libtool \
        zip \
        unzip \
        bzip2 \
        xz-utils \
        curl \
        ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# GNU Classpath's build looks for a Java compiler; ecj is the historically
# reliable choice.  JAVAC can be overridden at configure time if needed.
ENV JAVAC=ecj

# OpenJDK 8 BOOT JDK.  The OpenJDK 8 / IcedTea build must be bootstrapped with a
# JDK 7/8 -- the default-jdk above is JDK 21 (too new; OpenJDK 8's configure
# rejects it).  Ubuntu 24.04 (noble) no longer packages openjdk-8, so fetch the
# Eclipse Temurin 8 binary and install it at /opt/jdk8.  Pass to IcedTea as
# --with-jdk-home=$BOOT_JDK.
RUN mkdir -p /opt/jdk8 \
    && curl -fsSL -o /tmp/jdk8.tar.gz \
         "https://api.adoptium.net/v3/binary/latest/8/ga/linux/x64/jdk/hotspot/normal/eclipse" \
    && tar -xzf /tmp/jdk8.tar.gz -C /opt/jdk8 --strip-components=1 \
    && rm /tmp/jdk8.tar.gz \
    && /opt/jdk8/bin/java -version
ENV BOOT_JDK=/opt/jdk8

# clib4 v2.1 -- replace the base image's SDK clib4 with the official v2.1 release
# (AmigaLabs).  The amd64 .deb ships the cross-SDK at /usr/ppc-amigaos/SDK/clib4;
# relocate it over the toolchain SDK at /opt/ppc-amigaos/ppc-amigaos/SDK/clib4 so
# -mcrt=clib4 picks up v2.1 (libs/headers/sobjs incl. the CRT startup objects).
# The local Projects/clib4 overlay (mounted /clib4, also v2.1) still shadows the
# .a/.o + headers at build time; this provides the v2.1 .so for -use-dynld linking.
RUN curl -fsSL -o /tmp/clib4.deb \
      "https://github.com/AmigaLabs/clib4/releases/download/v2.1/clib4-v2.1_amd64.deb" \
    && dpkg-deb -x /tmp/clib4.deb /tmp/clib4dir \
    && rm -rf /opt/ppc-amigaos/ppc-amigaos/SDK/clib4 \
    && cp -a /tmp/clib4dir/usr/ppc-amigaos/SDK/clib4 /opt/ppc-amigaos/ppc-amigaos/SDK/ \
    && rm -rf /tmp/clib4dir /tmp/clib4.deb \
    && ls -l /opt/ppc-amigaos/ppc-amigaos/SDK/clib4/lib/libc.so
