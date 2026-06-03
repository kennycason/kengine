Area	Status
Physics tests	Zero test files
Tiled map perf	5-7ms/render, goal <1ms
Tween/easing system	None (only basic MoveAction/TimerAction)
General-purpose particle system	Only visual effects, no physics-based particles
True ECS	Entity system is minimal
TMX/TSX support	Only JSON format (TMJ/TSJ)
Scene management	No scene graph or scene transitions
Suggested Roadmap (Priority Order)
Fix the build — playdate gate + antfarm test fixes (your changes look correct)
macOS .app bundling — Gradle task to create a proper .app with embedded dylibs + assets
Tween/animation system — easing curves, property animation (essential for game feel)
Scene management — scene transitions, scene stack
Physics-based particle system — pooled, with lifespan/velocity/gravity
Asset embedding — compile assets into the binary for single-file distribution
Physics module tests — zero coverage currently
Tiled map performance — batch rendering optimization
