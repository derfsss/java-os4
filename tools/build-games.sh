#!/bin/sh
# Build the Java-OS4 example games: each games/*.java -> build/games/<Name>.jar
# (Java 8 bytecode, major 52 -- runs on the JamVM / OpenJDK 8 runtime).
#
# Each game is a single self-contained file with a public class <Name> and a
# `public static void main`.  On Java-OS4 run it with zero flags:
#     jamvm-openjdk -cp <Name>.jar <Name>
#
# Run inside the build image:
#   docker run --rm -v "<proj>:/work" -w /work javaos4-build:latest \
#       sh /work/tools/build-games.sh
set -e
JAVAC=javac          # default-jdk (21) cross-compiles to 8 via --release
JAR=jar
G=/work/games
B=/work/build/games
rm -rf "$B"; mkdir -p "$B"

for src in "$G"/*.java; do
    [ -e "$src" ] || { echo "no games/*.java found"; exit 1; }
    name=$(basename "$src" .java)
    echo "=== $name ==="
    mkdir -p "$B/$name"
    "$JAVAC" --release 8 -Xlint:-options -encoding UTF-8 -d "$B/$name" "$src"
    ( cd "$B/$name" && "$JAR" cfe "$B/$name.jar" "$name" . )
    echo "  $name.jar: $(find "$B/$name" -name '*.class' | wc -l) classes"
done

echo "=== build-games done ==="
ls -l "$B"/*.jar
