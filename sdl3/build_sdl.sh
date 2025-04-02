#!/bin/sh

cd SDL
mkdir build
cd build
cmake -DCMAKE_BUILD_TYPE=Release ..
cmake --build . --config Release --parallel
sudo cmake --install . --config Release
pkg-config --libs sdl3
otool -D /usr/local/lib/libSDL3.dylib
cd ../..


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
cd ../..


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
cd ../..


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
cd ../..


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
cd ../..