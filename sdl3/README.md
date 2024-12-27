# SDL3

Sub-Modules
- SDL
- SDL_image
- SDL_mixer
- SDL_net
- SDL_ttf

[Installation](https://wiki.libsdl.org/SDL3/Installation)

```shell
git submodule add https://github.com/libsdl-org/SDL SDL
cd SDL
mkdir build
cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
cmake --build . --config Release --parallel
sudo cmake --install . --config Release
pkg-config --libs sdl3
otool -D /usr/local/lib/libSDL3.dylib
```

```shell
git submodule add https://github.com/libsdl-org/SDL_image SDL_image
cd SDL_image
mkdir build
cd build
cmake .. \
  -DCMAKE_INSTALL_PREFIX=/usr/local \
  -DSDL3_IMAGE=ON \
  -DSDL3_INCLUDE_DIR=/usr/local/include/SDL3 \
  -DSDL3_LIBRARY=/usr/local/lib/libSDL3.dylib \
  -DINSTALL_PKGCONFIG=ON
make -j$(sysctl -n hw.ncpu)
sudo make install
pkg-config --libs sdl3-image
otool -D /usr/local/lib/libSDL3_image.dylib
```

```shell
git submodule add https://github.com/libsdl-org/SDL_mixer SDL_mixer
cd SDL_mixer
mkdir build
cd build
cmake .. \
  -DCMAKE_INSTALL_PREFIX=/usr/local \
  -DSDL3_MIXER=ON \
  -DSDL3_INCLUDE_DIR=/usr/local/include/SDL3 \
  -DSDL3_LIBRARY=/usr/local/lib/libSDL3.dylib \
  -DINSTALL_PKGCONFIG=ON
make -j$(sysctl -n hw.ncpu)
sudo make install
pkg-config --libs sdl3-mixer
otool -D /usr/local/lib/libSDL3_mixer.dylib
```

```shell
git submodule add https://github.com/libsdl-org/SDL_ttf SDL_ttf
cd SDL_ttf
mkdir build
cd build
cmake .. \
  -DCMAKE_INSTALL_PREFIX=/usr/local \
  -DSDL3_INCLUDE_DIR=/usr/local/include/SDL3 \
  -DSDL3_LIBRARY=/usr/local/lib/libSDL3.dylib
make -j$(sysctl -n hw.ncpu)
sudo make install
pkg-config --libs sdl3-ttf
otool -D /usr/local/lib/libSDL3_ttf.dylib
```

```shell
git submodule add https://github.com/libsdl-org/SDL_net SDL_net
cd SDL_net
mkdir build
cd build
cmake .. \
  -DCMAKE_INSTALL_PREFIX=/usr/local \
  -DSDL3_INCLUDE_DIR=/usr/local/include/SDL3 \
  -DSDL3_LIBRARY=/usr/local/lib/libSDL3.dylib
make -j$(sysctl -n hw.ncpu)
sudo make install
pkg-config --libs sdl3-net
otool -D /usr/local/lib/libSDL3_net.dylib
```


Misc unused notes/commands

```shell
export PKG_CONFIG_PATH="/usr/local/lib/pkgconfig:$PKG_CONFIG_PATH"
sudo install_name_tool -id @rpath/libSDL3_image.dylib /usr/local/lib/libSDL3_image.dylib
```
