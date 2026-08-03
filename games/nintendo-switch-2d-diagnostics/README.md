# Nintendo Switch 2D Diagnostics

Nintendo Switch 2D diagnostics NRO for validating Kengine's portable render, audio, input, lifecycle, and command-budget behavior.

Build:

```bash
./gradlew -Pkengine.enableNintendoSwitch=true :games:nintendo-switch-2d-diagnostics:buildSwitchNro
```

Output:

```text
games/nintendo-switch-2d-diagnostics/build/switch/nintendo-switch-2d-diagnostics.nro
```

Pages:

```text
Visual: alpha, tint, scale, clipping, offscreen sprites, sprite-sheet frames, lines
Text: glyph coverage, text scales, line fan, translucent fills
Audio: declared SFX overlap, music loop volume, music stop/restart
Perf: render-command budget meter, stress mode, overflow/drop counters
```

Controls:

```text
L / R: change diagnostics page
Up / Down: adjust performance stress count
A: play blip
B: play chord
Y: play noise
X: toggle music loop
Start: toggle stress mode
Select: reset diagnostics counters
Minus + Plus: exit Switch runtime
```

Normal multiplatform verification:

```bash
./gradlew :games:nintendo-switch-2d-diagnostics:allTests
```
