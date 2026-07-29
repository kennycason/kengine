# Hextris Desktop

SDL desktop host for the shared portable Hextris game in `:games:hextris-core`. This module owns the native executable and runtime asset copy tasks; gameplay, layout, sprite names, and asset declarations live in core and are loaded from `HextrisGame.assets`.

Run:

```bash
jenv exec ./gradlew :games:hextris-desktop:runDebugExecutableMacosArm64
```

Build:

```bash
jenv exec ./gradlew :games:hextris-desktop:linkDebugExecutableMacosArm64
```

The desktop window is 1280x720 to match the Switch layout.

Controls:

```text
Left/right arrows or A/D: move
Down arrow / S: soft drop
Up arrow / W: hard drop
Space / J / E / right shift: rotate clockwise
B / K / Y / I / Q / left shift: rotate counter-clockwise
X / U: rotate 180 degrees
Return / Escape: pause
Tab / Backspace: reset
```
