# SDL3

Sub-Modules
- SDL
- SDL_image

[Installation](https://wiki.libsdl.org/SDL3/Installation)

```shell
git submodule add https://github.com/libsdl-org/SDL sdl3/SDL
cd sdl3/SDL
mkdir build
cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
cmake --build . --config Release --parallel
sudo cmake --install . --config Release
```

```shell
git submodule add https://github.com/libsdl-org/SDL_image sdl3/SDL_image
cd sdl3/SDL_image
mkdir build
cd build
cmake .. \
  -DCMAKE_INSTALL_PREFIX=/usr/local \
  -DSDL3_IMAGE=ON \
  -DSDL3_INCLUDE_DIR=/usr/local/include/SDL3 \
  -DSDL3_LIBRARY=/usr/local/lib/libSDL3.dylib
make -j$(sysctl -n hw.ncpu)
sudo make install
```

```shell
sudo install_name_tool -id @rpath/libSDL3_image.dylib /usr/local/lib/libSDL3_image.dylib
```