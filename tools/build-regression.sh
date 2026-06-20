#!/bin/sh
# Build the Java regression suite.
#
#   tests/java8/  -> Java 8 bytecode (major 52)  -> build/suite8.jar
#                    runs on the current JamVM / OpenJDK 8 runtime
#   tests/java17/ -> Java 17 bytecode (major 61) -> build/suite17.jar
#                    for the OpenJDK 17 Zero port (rejected by the Java 8 VM)
#
# Each test is a standalone, self-verifying `main` (prints [PASS]/[FAIL] and a
# "<Class> RESULT: PASS|FAIL" line, exits 0 on success).  Tests live in packages
# java8.* / java17.* that mirror the JDK package they exercise.
#
# Run inside the build image:
#   docker run --rm -v "<proj>:/work" -w /work javaos4-build:latest \
#       sh /work/tools/build-regression.sh
set -e
JAVAC=javac          # default-jdk (21) cross-compiles via --release
JAR=jar
T=/work/tests
B=/work/build/regression
rm -rf "$B"; mkdir -p "$B/java8" "$B/java17"

echo "=== compiling java8 suite (--release 8) ==="
find "$T/java8" -name '*.java' > "$B/j8.list"
"$JAVAC" --release 8 -Xlint:-options -encoding UTF-8 -d "$B/java8" @"$B/j8.list"
( cd "$B/java8" && "$JAR" cf /work/build/suite8.jar . )
echo "  suite8.jar: $(find "$B/java8" -name '*.class' | wc -l) classes"

echo "=== compiling java17 suite (--release 17) ==="
find "$T/java17" -name '*.java' > "$B/j17.list"
"$JAVAC" --release 17 -encoding UTF-8 -d "$B/java17" @"$B/j17.list"
( cd "$B/java17" && "$JAR" cf /work/build/suite17.jar . )
echo "  suite17.jar: $(find "$B/java17" -name '*.class' | wc -l) classes"

# List the top-level test classes (FQNs) for the runner, one per line.
( cd "$B/java8"  && find . -name '*Test.class' ! -name '*$*' \
    | sed 's,^\./,,; s,/,.,g; s,\.class$,,' | sort ) > /work/build/suite8.tests
( cd "$B/java17" && find . -name '*Test.class' ! -name '*$*' \
    | sed 's,^\./,,; s,/,.,g; s,\.class$,,' | sort ) > /work/build/suite17.tests
echo "=== suite8 tests ($(wc -l < /work/build/suite8.tests)) ==="; cat /work/build/suite8.tests
echo "=== suite17 tests ($(wc -l < /work/build/suite17.tests)) ==="; cat /work/build/suite17.tests
echo "=== build-regression done ==="
