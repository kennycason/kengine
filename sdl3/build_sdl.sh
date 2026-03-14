#!/bin/sh
#
# Cross-platform SDL3 builder.
#
# macOS (hybrid): builds SDL3_mixer and SDL3_net from source.
#   SDL3, SDL3_image, SDL3_ttf installed via Homebrew.
#
# Linux: builds ALL SDL3 libs from source (not in apt repos yet).
#   Prerequisites: cmake, pkg-config, build-essential
#   sudo apt install cmake pkg-config build-essential libx11-dev libxext-dev \
#     libwayland-dev libxkbcommon-dev libasound2-dev libpulse-dev libpipewire-0.3-dev \
#     libfreetype-dev libharfbuzz-dev libjpeg-dev libpng-dev libwebp-dev
#
# Windows/MSYS2: builds ALL SDL3 libs from source.
#   Prerequisites: MSYS2 MINGW64 environment
#   pacman -S mingw-w64-x86_64-cmake mingw-w64-x86_64-gcc make
#
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OS="$(uname -s)"

# Detect platform
case "$OS" in
    Darwin)
        BREW_PREFIX="$(brew --prefix 2>/dev/null || echo /opt/homebrew)"
        SDL3_INCLUDE="${BREW_PREFIX}/include"
        SDL3_LIB="${BREW_PREFIX}/lib/libSDL3.dylib"
        NCPU="$(sysctl -n hw.ncpu)"
        INSTALL_PREFIX="/usr/local"
        USE_SUDO="sudo"
        ;;
    Linux)
        SDL3_INCLUDE="/usr/local/include"
        SDL3_LIB="/usr/local/lib/libSDL3.so"
        NCPU="$(nproc)"
        INSTALL_PREFIX="/usr/local"
        USE_SUDO="sudo"
        ;;
    MINGW*|MSYS*)
        SDL3_INCLUDE="/mingw64/include"
        SDL3_LIB="/mingw64/lib/libSDL3.dll.a"
        NCPU="$(nproc)"
        INSTALL_PREFIX="/mingw64"
        USE_SUDO=""
        ;;
    *)
        echo "Unsupported OS: $OS"
        exit 1
        ;;
esac

build_cmake_project() {
    local name="$1"
    local dir="$2"
    local extra_cmake_args="$3"

    echo ""
    echo "=== Building $name ==="
    cd "$SCRIPT_DIR/$dir"
    rm -rf build
    mkdir build
    cd build

    case "$OS" in
        MINGW*|MSYS*)
            cmake .. \
                -G "MinGW Makefiles" \
                -DCMAKE_MAKE_PROGRAM=mingw32-make \
                -DCMAKE_BUILD_TYPE=Release \
                -DCMAKE_INSTALL_PREFIX="$INSTALL_PREFIX" \
                $extra_cmake_args
            ;;
        *)
            cmake .. \
                -DCMAKE_BUILD_TYPE=Release \
                -DCMAKE_INSTALL_PREFIX="$INSTALL_PREFIX" \
                $extra_cmake_args
            ;;
    esac

    case "$OS" in
        MINGW*|MSYS*)
            mingw32-make -j"$NCPU"
            mingw32-make install
            ;;
        *)
            make -j"$NCPU"
            $USE_SUDO make install
            ;;
    esac

    echo "$name installed to $INSTALL_PREFIX"
}

# On Linux and Windows, build SDL3 core + all satellite libs from source
case "$OS" in
    Linux|MINGW*|MSYS*)
        build_cmake_project "SDL3" "SDL" ""

        if [ "$OS" = "Linux" ]; then
            sudo ldconfig
        fi

        build_cmake_project "SDL3_image" "SDL_image" ""
        build_cmake_project "SDL3_ttf" "SDL_ttf" ""
        ;;
esac

# All platforms: build mixer and net from source
SDL3_CMAKE_ARGS=""
if [ "$OS" = "Darwin" ]; then
    SDL3_CMAKE_ARGS="-DSDL3_INCLUDE_DIR=${SDL3_INCLUDE}/SDL3 -DSDL3_LIBRARY=${SDL3_LIB}"
fi

build_cmake_project "SDL3_mixer" "SDL_mixer" "$SDL3_CMAKE_ARGS"
build_cmake_project "SDL3_net" "SDL_net" "$SDL3_CMAKE_ARGS"

if [ "$OS" = "Linux" ]; then
    sudo ldconfig
fi

echo ""
echo "=== Done ==="
case "$OS" in
    Darwin)
        echo "Brew-managed: SDL3, SDL3_image, SDL3_ttf (update with: brew upgrade sdl3 sdl3_image sdl3_ttf)"
        echo "Source-built: SDL3_mixer, SDL3_net (installed to /usr/local/lib)"
        ;;
    *)
        echo "All SDL3 libs built from source and installed to $INSTALL_PREFIX"
        ;;
esac
