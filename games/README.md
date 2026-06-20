# Java-OS4 example games + apps

A small collection of self-contained **Java 8 Swing** programs that run on
Java-OS4 (JamVM + OpenJDK 8 on AmigaOS 4).  Each is a single source file with a
public class and `main`; all are mouse-driven Swing lightweights drawn with
Java2D, and launch with zero flags.

## Build

```
docker run --rm -v "<proj>:/work" -w /work javaos4-build:latest \
    sh /work/tools/build-games.sh
```

This compiles each `games/*.java` to `build/games/<Name>.jar` (Java 8 bytecode,
major 52) with the project's `javac --release 8`.

## Run (on Java-OS4)

```
jamvm-openjdk -cp <Name>.jar <Name>
```

(from the install dir, with `JAVA:` assigned — the same way any Java-OS4 app runs.)

## The programs

| Jar | What it is | Controls |
|-----|------------|----------|
| `TicTacToe`   | 3x3, vs an unbeatable minimax AI    | click an empty cell |
| `Checkers`    | 8x8 draughts, 2-player hotseat      | click a piece, then a highlighted square |
| `ConnectFour` | 7x6, vs a heuristic/minimax AI      | click a column to drop |
| `Minesweeper` | 9x9, 10 mines, first-click-safe     | left-click reveal, right-click flag |
| `MemoryMatch` | 4x4 concentration (8 pairs)         | click cards to flip |
| `NtpClock`    | 24h 7-segment digital clock         | "Sync now" button |

Each program has a "New Game" (or "Sync now") control and a status line.

## NtpClock — note on the time source

`NtpClock` tries to fetch the time from an NTP server over SNTP/UDP and falls
back to the Amiga system clock if that fails.  On current Java-OS4 the networking
layer (`libnet`) is a **stub**, so `DatagramSocket` throws `UnsatisfiedLinkError`
and the clock shows the **Amiga system / RTC time** (the status line reports
which source is live).  To get NTP-accurate time today, run an OS-level NTP
client (Roadshow/AmiTCP) that keeps the system clock set; the in-app SNTP path
will start working once a real `libnet` (over `bsdsocket.library`) is built.

The display repaints only when the shown second changes (~1 Hz), so it stays
light on the interpreter and does not contend with the pointer / window manager.
