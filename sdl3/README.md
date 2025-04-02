# SDL3

Sub-Modules
- SDL
- SDL_image
- SDL_mixer
- SDL_net
- SDL_ttf

[Installation](https://wiki.libsdl.org/SDL3/Installation)

If when cloning the repo you did not add the `--recursive-submodules` flag, then you will need to run :

```shell
git submodule update --init --recursive
```

and then:

```shell
git submodule update --remote
```

## Build SDL3

Required packages:
- cmake
- pkg-config

Install via `brew install cmake pkg-config`

```shell
bash build_sdl.sh
```

```shell
cd /Users/kenny/code/kengine/kengine/build/bin/native/Frameworks
ln -s libSDL3_net.dylib libSDL3_net.0.dylib
```

## Misc unused notes/commands

```shell
export PKG_CONFIG_PATH="/usr/local/lib/pkgconfig:$PKG_CONFIG_PATH"
sudo install_name_tool -id @rpath/libSDL3_image.dylib /usr/local/lib/libSDL3_image.dylib
```


## Initial project creation (Do not need to do again)

```shell
git submodule add https://github.com/libsdl-org/SDL SDL
git submodule add https://github.com/libsdl-org/SDL_image SDL_image
git submodule add https://github.com/libsdl-org/SDL_mixer SDL_mixer
git submodule add https://github.com/libsdl-org/SDL_ttf SDL_ttf
git submodule add https://github.com/libsdl-org/SDL_net SDL_net
```
