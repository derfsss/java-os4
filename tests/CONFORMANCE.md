# Amiga VM conformance — `java8` suite

Results of running `tests/java8/` on the shipping runtime (JamVM 2.0 + OpenJDK 8)
under QEMU (`-M amigaone`, MPC 7447/7457 G4), 2026-06-20. The host reference JDK
runs all 25 `java8` + `java17` tests green (`tools/run-regression.sh`); this page
records how the **`java8`** suite fares on the actual VM.

## Summary — 14 / 16 pass; ~760 assertions, one functional gap

| test | JDK package | VM result |
|---|---|---|
| LanguageTest | java.lang | ✅ PASS |
| StringTest | java.lang | ✅ 42/42 |
| MathNumberTest | java.lang | ✅ 46/46 |
| LambdaTest | java.lang.invoke | ✅ 30/30 |
| CollectionsTest | java.util | ✅ 56/56 |
| ArraysTest | java.util | ✅ 42/42 |
| StreamTest | java.util.stream | ✅ 56/56 |
| RegexTest | java.util.regex | ✅ 43/43 |
| BigNumberTest | java.math | ✅ 45/45 |
| TimeTest | java.time | ✅ 35/35 |
| TextTest | java.text | ✅ 20/20 |
| IoTest | java.io | ✅ 25/25 |
| NioTest | java.nio | ✅ 41/41 |
| ConcurrencyTest | java.util.concurrent | ✅ 46/46 |
| MiscUtilTest | java.util | ⚠️ 52/53 |
| ReflectionTest | java.lang.reflect | ✅ 42/42 |

The full Java 8 surface — language, strings, math, lambdas/method-refs, the whole
collection + stream + `Collectors` API, regex, `BigInteger`/`BigDecimal`,
`java.time`, `java.text`, byte/char IO + serialization, NIO buffers + `Files`, and
the entire `java.util.concurrent` stack (threads, executors, `CompletableFuture`,
atomics, locks, `ConcurrentHashMap`, latches/barriers/semaphores) — runs correctly
on the VM.

## Findings

1. **SecureRandom / entropy native missing (the one functional gap).**
   `UUID.randomUUID()` — and anything backed by `SecureRandom` — throws
   `UnsatisfiedLinkError: init`; the Amiga port has no entropy native. This is the
   single failing assertion across the suite. Seeded `java.util.Random` works (its
   deterministic sequence is verified). Same root cause as the
   `File.createTempFile` gap seen with `VmSuite`. `MiscUtilTest` guards the call so
   the gap is one contained failure rather than aborting the test.

2. **Intermittent batch hang (VM stability, not a class-library gap).** Every test
   passes when run in its own `jamvm-openjdk` process. Run back-to-back in one
   script, one test occasionally hangs and wedges the batch (observed:
   `ReflectionTest` in one run, `MiscUtilTest` in another) — no GrimReaper crash, the
   process just stops and never exits. Both reproduce as PASS when re-run alone, so
   this is an intermittent JamVM/clib4 issue (an exit-path or a `SecureRandom`
   block are the leading suspects), not missing functionality. Mitigation: run each
   test in a separate process (the host runner does; an Amiga timeout-runner that
   survives a hung test is a TODO). `ReflectionTest` flushes `System.out` per check
   so a hang leaves the last-completed check visible in the log.

## Reproducing

```
# host reference check (all 25 green):
docker run --rm -v "<proj>:/work" -w /work javaos4-build:latest sh /work/tools/run-regression.sh

# on the VM (deploy build/suite8.jar to SYS:Test, CWD=SYS:Test):
jamvm-openjdk -cp suite8.jar java8.java.util.stream.StreamTest   # one test
```
