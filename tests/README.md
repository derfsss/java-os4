# Java-OS4 test suite

Self-verifying Java programs that exercise the VM + class library. Every test is a
standalone `main` that prints `[PASS]`/`[FAIL]` per check and a final
`<Class> RESULT: PASS|FAIL` line, and exits `0` on success / `1` on any failure.

## Layout

```
tests/
  java8/    API + language regression suite, Java 8     — 16 tests, package java8.*
  java17/   API + language regression suite, Java 17    —  9 tests, package java17.*
  regression/  project regression tests (the forum-0.5.x fixes + a broad smoke)
  gui/         AWT/Swing programs (need a display — run on the Amiga toolkit)
  conformance/ the Phase-3 bring-up / headless conformance programs
  native/      native (C) helpers
```

### `java8/` and `java17/` — the package-organized suite

Directories mirror the JDK package each test exercises, e.g.
`java8/java/util/stream/StreamTest.java` is class `java8.java.util.stream.StreamTest`
(the `java8.`/`java17.` prefix keeps the test classes out of the real `java.*`
namespace, which you may not declare).

- **`java8/`** is compiled to Java 8 bytecode (class-file major 52) and **runs on the
  current JamVM / OpenJDK 8 runtime** — this is the conformance suite for the shipping
  VM.
- **`java17/`** is compiled to Java 17 bytecode (major 61) and covers Java 9–17
  language + API features (records, sealed types, switch expressions, text blocks,
  pattern matching, `List.of`, `Stream.toList`, `Files.readString`, …). It is for the
  **OpenJDK 17 Zero port**; the Java 8 VM rejects it up front with
  `UnsupportedClassVersionError` (by design — see `regression/VersionGateTest`).

| package | java8 | java17 |
|---|---|---|
| `java.lang` (language, String, Math/Number) | LanguageTest, StringTest, MathNumberTest | RecordsTest, SealedTest, SwitchExprTest, TextBlockTest, PatternMatchTest, StringNewTest |
| `java.lang.invoke` (lambdas/functions) | LambdaTest | |
| `java.lang.reflect` | ReflectionTest | |
| `java.util` (collections, misc) | CollectionsTest, ArraysTest, MiscUtilTest | CollectionsNewTest |
| `java.util.stream` | StreamTest | StreamNewTest |
| `java.util.concurrent` | ConcurrencyTest | |
| `java.util.regex` | RegexTest | |
| `java.io` / `java.nio` | IoTest, NioTest | FilesNewTest |
| `java.time` / `java.text` / `java.math` | TimeTest, TextTest, BigNumberTest | |

### `regression/` — project regression tests

- `VmSuite` — broad VM smoke (lang, collections, streams, reflection, threads, IO).
- `CloseTest` — fix #1: a Swing `EXIT_ON_CLOSE` frame must close + exit, not hang.
- `KeyBindTest` — fix #4: `WHEN_IN_FOCUSED_WINDOW` key bindings must fire.
- `VersionGateTest` — fix #5: a major-53 class must be rejected with
  `UnsupportedClassVersionError`.

## Building + running

Build (and self-check on the image's reference JDK):

```sh
docker run --rm -v "<proj>:/work" -w /work javaos4-build:latest \
    sh /work/tools/run-regression.sh        # build + run + "ALL GREEN"
```

`tools/build-regression.sh` alone produces `build/suite8.jar`, `build/suite17.jar`
and the test-class lists `build/suite8.tests` / `build/suite17.tests`.

Run a single test (host or Amiga):

```sh
java -cp suite8.jar java8.java.util.stream.StreamTest
```

On the Amiga, deploy `suite8.jar` and run the java8 tests with the `java` launcher,
one FQN per `build/suite8.tests` line; a non-`PASS` `RESULT:` line marks a gap in the
VM/class library. The `gui/` programs run under the native `sun.awt.amiga` toolkit;
`regression/` and `conformance/` are built into `build/testsuite.zip` by
`tools/build-tests.sh`.

## Conventions for new tests

- Put it under `java8/<pkg-path>/` or `java17/<pkg-path>/` with
  `package java8.<pkg>;` / `package java17.<pkg>;` matching the directory.
- Self-verify with the `ck(name, got, expected)` helper; assert known values, stay
  deterministic (fixed seeds, `Locale.US`, no wall-clock), headless, no network.
- File/IO tests use a **relative** file named after the class and delete it; do **not**
  use `File.createTempFile` or `SecureRandom` (the Amiga runtime lacks the entropy
  native) or absolute paths.
