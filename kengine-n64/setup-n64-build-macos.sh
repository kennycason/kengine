#!/usr/bin/env bash

set -euo pipefail

log() {
    printf '%s\n' "$*"
}

fail() {
    printf 'error: %s\n' "$*" >&2
    exit 1
}

N64_INST="${N64_INST:-/opt/libdragon}"

log "Kengine Nintendo 64 build environment setup (macOS)"
log ""
log "This script installs the libdragon N64 toolchain."
log "Target directory: $N64_INST"
log ""

require_command() {
    command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"
}

require_command git
require_command make
require_command gcc

if [ -d "$N64_INST/bin" ] && [ -x "$N64_INST/bin/mips64-elf-gcc" ]; then
    log "libdragon toolchain already installed at $N64_INST"
    log "mips64-elf-gcc: $($N64_INST/bin/mips64-elf-gcc --version | head -1)"
    log ""
    log "To rebuild, remove $N64_INST and re-run this script."
    exit 0
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIBDRAGON_CHECKOUT="${LIBDRAGON_CHECKOUT:-$SCRIPT_DIR/../sdl3/libdragon}"

if [ ! -d "$LIBDRAGON_CHECKOUT/.git" ]; then
    LIBDRAGON_CHECKOUT="$(mktemp -d)/libdragon"
    log "Cloning libdragon to $LIBDRAGON_CHECKOUT"
    git clone --depth=1 https://github.com/DragonMinded/libdragon.git "$LIBDRAGON_CHECKOUT"
fi

log "Building libdragon toolchain from $LIBDRAGON_CHECKOUT"
log "This may take a while (building GCC cross-compiler for MIPS)..."

export N64_INST
cd "$LIBDRAGON_CHECKOUT/tools"

if [ -f "build-toolchain.sh" ]; then
    bash build-toolchain.sh
else
    log "Toolchain build script not found. Trying legacy build..."
    cd "$LIBDRAGON_CHECKOUT"
    make toolchain
fi

cd "$LIBDRAGON_CHECKOUT"
make install

log ""
log "libdragon toolchain installed to $N64_INST"
log "mips64-elf-gcc: $($N64_INST/bin/mips64-elf-gcc --version | head -1)"
log ""
log "Add to your shell profile:"
log "  export N64_INST=$N64_INST"
log "  export PATH=\$N64_INST/bin:\$PATH"
