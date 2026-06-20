#!/bin/sh
# Build + run the Java regression suite on the build image's reference JDK.
# This is the "is the suite itself green" gate -- it must pass on a correct JVM
# before the suite is trusted to judge the Amiga VM.  (The java8 suite is also
# deployable to the Amiga as build/suite8.jar; java17 targets the Zero port.)
#
#   docker run --rm -v "<proj>:/work" -w /work javaos4-build:latest \
#       sh /work/tools/run-regression.sh
set -e
sh /work/tools/build-regression.sh
cd /work/build
pass=0; fail=0; faillist=""
for suite in suite8 suite17; do
  echo "=== running $suite on reference JDK ==="
  while read t; do
    out=$(java -cp "$suite.jar" "$t" 2>&1)
    if echo "$out" | grep -q "RESULT: PASS"; then
      pass=$((pass+1)); printf "  PASS  %s\n" "$t"
    else
      fail=$((fail+1)); faillist="$faillist $t"; printf "  FAIL  %s\n" "$t"
      echo "$out" | grep -E "\[FAIL\]|Exception" | sed 's/^/        /' | head -5
    fi
  done < "$suite.tests"
done
echo "==== $pass passed, $fail failed ===="
if [ -n "$faillist" ]; then echo "FAILURES:$faillist"; exit 1; fi
echo "ALL GREEN"
