# SDL3 Setup

Kengine uses a hybrid approach for SDL3 dependencies:
- **Homebrew** on macOS for SDL3, SDL3_image, SDL3_ttf, SDL3_mixer, and SDL3_net
- **Built from source** for Linux and Windows, where CI still needs source-managed SDL3 libraries

## Prerequisites

```shell
brew install cmake pkg-config
```

## Install SDL3 Libraries

### 1. Install Homebrew packages

```shell
brew install sdl3 sdl3_image sdl3_ttf sdl3_mixer sdl3_net
```

To update later:
```shell
brew upgrade sdl3 sdl3_image sdl3_ttf sdl3_mixer sdl3_net
```

### 2. Build SDL3 libraries from source

If cloning the repo fresh, initialize the submodules first:
```shell
git submodule update --init --recursive
```

Then build and install:
```shell
bash sdl3/build_sdl.sh
```

This is required for Linux and Windows CI. On macOS, Homebrew packages are preferred; this script remains available as a fallback source build.

## Library Locations

| Library | Source | Headers | Libs |
|---------|--------|---------|------|
| SDL3 | Homebrew | `/opt/homebrew/include` | `/opt/homebrew/lib` |
| SDL3_image | Homebrew | `/opt/homebrew/include` | `/opt/homebrew/lib` |
| SDL3_ttf | Homebrew | `/opt/homebrew/include` | `/opt/homebrew/lib` |
| SDL3_mixer | Homebrew | `/opt/homebrew/include` | `/opt/homebrew/lib` |
| SDL3_net | Homebrew | `/opt/homebrew/include` | `/opt/homebrew/lib` |

## Git Submodules

The SDL submodules are still needed for Linux and Windows source builds:
- `sdl3/SDL` — https://github.com/libsdl-org/SDL
- `sdl3/SDL_image` — https://github.com/libsdl-org/SDL_image
- `sdl3/SDL_ttf` — https://github.com/libsdl-org/SDL_ttf
- `sdl3/SDL_mixer` — https://github.com/libsdl-org/SDL_mixer
- `sdl3/SDL_net` — https://github.com/libsdl-org/SDL_net

## Updating Source-Built Libraries

```shell
cd sdl3/SDL && git pull origin main && cd ../..
cd sdl3/SDL_image && git pull origin main && cd ../..
cd sdl3/SDL_ttf && git pull origin main && cd ../..
cd sdl3/SDL_mixer && git pull origin main && cd ../..
cd sdl3/SDL_net && git pull origin main && cd ../..
bash sdl3/build_sdl.sh
```
