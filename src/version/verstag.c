/*
 * AmigaOS "$VER:" version cookie for the Java-OS4 launcher.
 *
 * Read by the AmigaDOS `Version` command (e.g. `Version JAVA:jamvm-openjdk`).
 * The version proper is the Java-OS4 project version (a clean major.minor the
 * Version command can parse); the OpenJDK class-library version follows as
 * free text, shown by `Version ... FULL`.  All three values are -D'd from the
 * build (VERSION file, class-library JDK, build date).
 *
 * GPLv2 (java-os4 project).
 */
#ifndef JAVAOS4_VER
#define JAVAOS4_VER "0.0"
#endif
#ifndef JAVAOS4_JAVAVER
#define JAVAOS4_JAVAVER "1.8.0"
#endif
#ifndef JAVAOS4_DATE
#define JAVAOS4_DATE "01.01.2026"
#endif

const char __attribute__((used)) java_os4_verstag[] =
    "$VER: Java-OS4 " JAVAOS4_VER " (" JAVAOS4_DATE ") OpenJDK " JAVAOS4_JAVAVER;
