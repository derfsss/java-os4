#!/bin/sh
# Fetch the vendored upstream source trees Java-OS4 builds from.
#
# These trees are large and gitignored -- only OUR changes are tracked (as the
# patch in docs/) -- so a fresh clone needs this one-time step.  Everything here
# is PUBLIC:
#   * vendor/jamvm     <- github.com/jaokim/jamiga-jamvm
#                         + docs/jamvm-amiga-openjdk.patch  (our 11 commits)
#   * vendor/icedtea8  <- github.com/jaokim/jamiga-icedtea8-3.0
#
# Run from the repo root:   sh tools/fetch-vendor.sh      (or:  make vendor)
# Needs: git + network (runs on the host, not in the build container).
set -e
cd "$(dirname "$0")/.."                 # repo root

# --- JamVM (the engine): jaokim's JAmiga fork + our AmigaOS4 / OpenJDK 8 work --
if [ -d vendor/jamvm/.git ]; then
    echo "=== vendor/jamvm already present -- skipping ==="
else
    echo "=== cloning jamiga-jamvm -> vendor/jamvm ==="
    git clone https://github.com/jaokim/jamiga-jamvm vendor/jamvm
    echo "=== applying docs/jamvm-amiga-openjdk.patch (our AmigaOS4 + OpenJDK 8 changes) ==="
    git -C vendor/jamvm am --3way < docs/jamvm-amiga-openjdk.patch
    echo "    vendor/jamvm now at: $(git -C vendor/jamvm log --oneline -1)"
fi

# --- IcedTea 8 harness: produces the OpenJDK 8 source the natives compile from --
if [ -d vendor/icedtea8/.git ]; then
    echo "=== vendor/icedtea8 already present -- skipping ==="
else
    echo "=== cloning jamiga-icedtea8-3.0 -> vendor/icedtea8 ==="
    git clone https://github.com/jaokim/jamiga-icedtea8-3.0 vendor/icedtea8
fi

cat <<'EOF'

=== vendor sources ready ===
  vendor/jamvm     -- the JamVM engine (with our AmigaOS 4 + OpenJDK 8 patch)
  vendor/icedtea8  -- the IcedTea 8 build harness

ONE heavyweight step remains before `make natives`: build the OpenJDK 8 source
tree the native libraries compile from.  Run the IcedTea harness (see
vendor/icedtea8/README + HACKING) to download + extract OpenJDK 8u into
build/openjdk8/.  The native build scripts expect that source at
build/openjdk8/jdk-<changeset>; set J= in tools/build-openjdk-natives.sh and
tools/build-awt-natives.sh to match the directory it produces.

The VM (`make vm`) and clib4 (`make clib4`) do NOT need that step -- only the
OpenJDK native libraries do.  See docs/BUILDING.md for the full flow.
EOF
