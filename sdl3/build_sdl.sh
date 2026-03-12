#!/bin/sh
#
# Build SDL3_mixer and SDL3_net from source.
# SDL3, SDL3_image, and SDL3_ttf are installed via Homebrew:
#   brew install sdl3 sdl3_image sdl3_ttf
#
# Prerequisites: cmake, pkg-config
#   brew install cmake pkg-config
#
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BREW_PREFIX="$(brew --prefix 2>/dev/null || echo /opt/homebrew)"
SDL3_INCLUDE="${BREW_PREFIX}/include"
SDL3_LIB="${BREW_PREFIX}/lib/libSDL3.dylib"
NCPU="$(sysctl -n hw.ncpu)"

echo "=== Building SDL3_mixer ==="
cd "$SCRIPT_DIR/SDL_mixer"
rm -rf build
mkdir build
cd build
cmake .. \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_INSTALL_PREFIX=/usr/local \
  -DSDL3_INCLUDE_DIR="$SDL3_INCLUDE/SDL3" \
  -DSDL3_LIBRARY="$SDL3_LIB"
make -j"$NCPU"
sudo make install
echo "SDL3_mixer installed:"
otool -D /usr/local/lib/libSDL3_mixer.dylib

echo ""
echo "=== Building SDL3_net ==="
cd "$SCRIPT_DIR/SDL_net"
rm -rf build
mkdir build
cd build
cmake .. \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_INSTALL_PREFIX=/usr/local \
  -DSDL3_INCLUDE_DIR="$SDL3_INCLUDE/SDL3" \
  -DSDL3_LIBRARY="$SDL3_LIB"
make -j"$NCPU"
sudo make install
echo "SDL3_net installed:"
otool -D /usr/local/lib/libSDL3_net.dylib

echo ""
echo "=== Done ==="
echo "Brew-managed: SDL3, SDL3_image, SDL3_ttf (update with: brew upgrade sdl3 sdl3_image sdl3_ttf)"
echo "Source-built: SDL3_mixer, SDL3_net (installed to /usr/local/lib)"
