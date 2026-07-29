# Hextris Switch

Nintendo Switch NRO host for the shared portable Hextris game in `:games:hextris-core`. This module declares only Switch artifact metadata; source and assets are imported from core through `gameSourceProject(...)` and `assetsFrom(...)`.

Build:

```bash
jenv exec ./gradlew -Pkengine.switch=true :games:hextris-switch:buildSwitchNro
```

Output:

```text
games/hextris-switch/build/switch/hextris-switch.nro
```

Controls:

```text
D-pad left/right: move
D-pad down: soft drop
D-pad up: hard drop
A / R: rotate clockwise
B / L / Y: rotate counter-clockwise
X: rotate 180 degrees
Start: pause
Select: reset
Minus + Plus: exit Switch runtime
```
