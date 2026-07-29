# Hextris Switch

Portable Hextris prototype for the experimental Nintendo Switch backend. This module keeps the existing desktop Hextris untouched while exercising the shared `PortableGame`, `InputState`, `AudioContext`, `RenderContext`, sprite, text, and music paths.

Run the desktop SDL version:

```bash
jenv exec ./gradlew :games:hextris-switch:runDebugExecutableMacosArm64
```

Build the Switch NRO:

```bash
jenv exec ./gradlew -Pkengine.switch=true :games:hextris-switch:buildSwitchNro
```

The Switch build embeds `sound/techno_boss_worm.ogg` as looped background music. The backend converts it to 48 kHz stereo PCM during the Gradle build.

Output:

```text
games/hextris-switch/build/switch/hextris-switch.nro
```

Controls:

```text
D-pad / arrows / WASD: move and soft drop
Up: hard drop
A / R: rotate clockwise
B / L / Y: rotate counter-clockwise
X: rotate 180 degrees
Start: pause
Select: reset
Minus + Plus: exit Switch runtime
```
