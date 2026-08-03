# Nintendo Switch Demo

Pure Kotlin demo game used to exercise both the normal Kengine SDL host and the experimental Nintendo Switch backend.
The desktop build registers `assets/sprites/pokeball.bmp` and `assets/sprites/block_sprites.png` through the normal Kengine `SpriteContext`; the Switch build declares the same assets through the reusable `sprite` and `spriteSheet` DSL, then renders the portable sprite commands through the software framebuffer backend.

Run/build the desktop Kengine SDL version with the normal native executable tasks:

```bash
./gradlew :games:nintendo-switch-demo:runDebugExecutableMacosArm64
```

Desktop controls:

```text
Arrow keys / WASD: move the square
Space / J / controller A: shift the color palette faster
B / K / controller B: pulse the square size
X / U / controller X: shift the color palette faster
Y / I / controller Y: reverse the palette shift
Q / left shift / controller L1: slow manual movement
E / right shift / controller R1: speed up manual movement
Tab / backspace / controller select: perturb the checksum
```

Build the game-facing NRO with the Switch backend enabled:

```bash
./gradlew -Pkengine.enableNintendoSwitch=true :games:nintendo-switch-demo:buildSwitchNro
```

Output:

```text
games/nintendo-switch-demo/build/switch/nintendo-switch-demo.nro
```

The low-level libnx host still lives in `:kengine-nintendo-switch`; this module owns the portable game source and the game-facing artifact path.
In Ryujinx, use the same D-pad / left stick and face/shoulder controls. Press `-` and `+` together to exit the Switch runtime.
