# External Dependencies

## Box2D

Setup notes:

```shell
cd kengine/
git clone https://github.com/erincatto/box2d.git external/box2d
cd external/box2d/
mkdir build && cd build
cd external/box2d
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release -DBOX2D_BUILD_EXAMPLES=OFF -DBOX2D_BUILD_UNIT_TESTS=OFF
cmake --build . --config Release
```