# Kengine Math

Small common Kotlin math and data-structure primitives that are safe for `:kengine-core`, Nintendo Switch, Nintendo 64, Playdate, desktop, JVM, and JS code.

This module should stay free of SDL, GPU, file-system, and platform runtime dependencies. Keep it focused on value types, deterministic helpers, and containers that make game logic easier to share across platform backends.

Initial scope:

- `Point2` / `Point3` for integer grid and tile coordinates.
- `Vec2` / `Vec3` for double-precision gameplay math. `Vector2` / `Vector3` remain aliases for compatibility, but new APIs should prefer the shorter names.
- `FloatVector2` / `FloatVector3` for renderer-facing or compact float math.
- `List2D` / `Array2D` for rectangular 2D data with `grid[x][y]` and `grid[x, y]` access.
- `IntVec2`, `Rect`, `IntRect`, math constants, and numeric extensions that used to live under `:kengine`.

Planned later, when proven by N64/3D renderer work:

- Matrix types and transforms.
- Quaternion/angle helpers where rotation math starts repeating.
- Bounded numeric helpers and interpolation/easing helpers where they are useful outside one subsystem.
