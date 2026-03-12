# SDL3 Setup

Kengine uses a hybrid approach for SDL3 dependencies:
- **Homebrew** for SDL3, SDL3_image, SDL3_ttf (stable releases, easy updates)
- **Built from source** for SDL3_mixer and SDL3_net (no Homebrew formula available)

## Prerequisites

```shell
brew install cmake pkg-config
```

## Install SDL3 Libraries

### 1. Install Homebrew packages

```shell
brew install sdl3 sdl3_image sdl3_ttf
```

To update later:
```shell
brew upgrade sdl3 sdl3_image sdl3_ttf
```

### 2. Build SDL3_mixer and SDL3_net from source

If cloning the repo fresh, initialize the submodules first:
```shell
git submodule update --init --recursive
```

Then build and install:
```shell
bash sdl3/build_sdl.sh
```

This builds SDL3_mixer and SDL3_net from the git submodules and installs them to `/usr/local/lib`.

## Library Locations

| Library | Source | Headers | Libs |
|---------|--------|---------|------|
| SDL3 | Homebrew | `/opt/homebrew/include` | `/opt/homebrew/lib` |
| SDL3_image | Homebrew | `/opt/homebrew/include` | `/opt/homebrew/lib` |
| SDL3_ttf | Homebrew | `/opt/homebrew/include` | `/opt/homebrew/lib` |
| SDL3_mixer | Source build | `/usr/local/include` | `/usr/local/lib` |
| SDL3_net | Source build | `/usr/local/include` | `/usr/local/lib` |

## Git Submodules

Only SDL_mixer and SDL_net submodules are needed for the source builds:
- `sdl3/SDL_mixer` — https://github.com/libsdl-org/SDL_mixer
- `sdl3/SDL_net` — https://github.com/libsdl-org/SDL_net

The SDL, SDL_image, and SDL_ttf submodules are retained for reference but are not used in the build process — Homebrew packages are used instead.

## Updating Source-Built Libraries

```shell
cd sdl3/SDL_mixer && git pull origin main && cd ../..
cd sdl3/SDL_net && git pull origin main && cd ../..
bash sdl3/build_sdl.sh
```
