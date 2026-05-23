# Kengine Architecture

## Overview

Kengine is a Kotlin/Native 2D game engine built on SDL3. It uses a **functional, hooks-based architecture** inspired by React for state management and a **context-based dependency injection** pattern for subsystem access.

## Core Loop

```
GameRunner(frameRate = 60) { MyGame() }
    └── GameLoop.start()
            ├── pollEvents()        → SDLEventContext dispatches to input contexts
            ├── game.update()       → Game logic, physics, state changes
            ├── game.draw()         → Rendering via SDL3
            └── frame pacing        → Delta time, sleep to hit target FPS
```

### Game Interface

Every game implements three methods:

```kotlin
interface Game {
    fun update()   // Called each frame for logic
    fun draw()     // Called each frame for rendering
    fun cleanup()  // Called on shutdown
}
```

## Context System

The engine is organized as a set of **Context** objects, each managing one subsystem. Contexts are created during `GameContext.create()` and accessed via hook-style functions.

### Context Hierarchy

```
GameContext (singleton, owns all subsystems)
├── SDLContext           → Window, renderer, screen management
├── SDLEventContext      → Raw SDL event polling and dispatch
├── ClockContext         → Delta time, frame timing, intervals
├── KeyboardContext      → Key state tracking
├── MouseContext         → Mouse position, buttons, scroll
├── ControllerContext    → Gamepad/joystick support (Xbox, PS, Switch, etc.)
├── SpriteContext        → Sprite loading and rendering
├── TextureContext       → Texture management and caching
├── FontContext          → TTF font loading and text rendering
├── GeometryContext      → Primitive drawing (lines, rects, circles)
├── ViewContext          → Flex-based UI layout system
├── EventContext         → Application-level event bus
├── ActionContext        → Scheduled actions (timers, intervals, tweens)
├── LoggerContext        → Configurable logging
└── (optional modules)
    ├── SoundContext     → Audio playback (kengine-sound)
    ├── PhysicsContext   → Chipmunk 2D physics (kengine-physics)
    └── NetworkContext   → TCP/UDP networking (kengine-network)
```

### Accessing Contexts

Two patterns for context access:

```kotlin
// Block-scoped (preferred) — auto-cleanup support
useGameContext {
    useSpriteContext {
        addSprite("player", "assets/player.png")
    }
}

// Direct access (for one-off reads)
val clock = getClockContext()
val dt = clock.deltaTime
```

## Hooks System (kengine-reactive)

React-inspired hooks for managing state in a functional style:

| Hook | Purpose |
|------|---------|
| `useState(initial)` | Reactive state variable with get/set |
| `useEffect(deps) { ... }` | Side effects that run when dependencies change |
| `useContext<T> { ... }` | Scoped context access with optional cleanup |
| `useMemo(deps) { ... }` | Memoized computation |
| `useReducer(reducer, initial)` | State machine / complex state transitions |

## SDL3 Integration

SDL3 is integrated via Kotlin/Native **cinterop** — C header bindings generated at compile time.

### Interop Definitions

Located in `kengine/src/nativeInterop/cinterop/`:
- `sdl3.def` → Core SDL3 (windowing, events, rendering)
- `sdl3_image.def` → Image loading (PNG, JPG, WebP)
- `sdl3_ttf.def` → TrueType font rendering
- `sdl3_mixer.def` → Audio mixing (in kengine-sound)
- `sdl3_net.def` → Networking (in kengine-network)

### SDL3 Source Build

SDL3 libraries are built from source via git submodules:
```
sdl3/
├── SDL/            → Core SDL3
├── SDL_image/      → Image codec support
├── SDL_mixer/      → Audio mixer
├── SDL_net/        → Network sockets
├── SDL_ttf/        → Font rendering
└── build_sdl.sh    → Cross-platform build script
```

## Build System

### Gradle Plugins (buildSrc/)

| Plugin | Purpose |
|--------|---------|
| `PlatformConfig` | Detects OS/arch, provides compiler/linker flags |
| `KengineAssetPlugin` | Copies game assets to build output |
| `SdlDylibPlugin` | Manages SDL3 shared library copying |
| `KengineNativePlugin` | Common native compilation setup |

### Platform Detection

The build auto-detects the host platform and selects the appropriate Kotlin/Native target:
- macOS arm64 → `macosArm64("native")`
- macOS x64 → `macosX64("native")`
- Linux arm64 → `linuxArm64("native")`
- Linux x64 → `linuxX64("native")`
- Windows → `mingwX64("native")`

## Module Dependency Graph

```
games/*
└── kengine (core)
    ├── kengine-reactive (hooks)
    └── (optional) kengine-sound, kengine-physics, kengine-network

kengine-test (test utilities, used by all module tests)
kengine-playdate (standalone, ARM32 target)
```

## Key Design Decisions

1. **Composition over inheritance**: No deep class hierarchies. Game objects compose behaviors via hooks and contexts
2. **Native-first**: Primary target is Kotlin/Native with direct C interop to SDL3. JVM/JS targets exist for non-SDL code sharing
3. **Functional patterns**: useState/useEffect bring React-like ergonomics to game development
4. **Context as DI**: Instead of a DI framework, the context stack provides scoped access to engine services
5. **Modular architecture**: Optional features (sound, physics, network) are separate modules with their own SDL3 bindings

## Package Organization (kengine core)

```
com.kengine/
├── action/        Scheduled actions (timers, intervals, move tweens)
├── entity/        Base Entity class for game objects
├── event/         Event bus for decoupled communication
├── font/          TTF font loading and text rendering
├── geometry/      Primitive drawing (lines, rects, circles, polygons)
├── graphics/      Sprites, textures, animation, sprite batching, color
├── input/         Keyboard, mouse, and controller (with per-device mappings)
├── log/           Logger with configurable levels and stream builder
├── map/tiled/     Tiled map loader (TMJ/TMX format)
├── math/          Vec2, Vec3, Rect, IntVec2, IntRect, math extensions
├── particle/      Particle effects (smoke, rainbow, waveform, sacred geometry)
├── sdl/           Low-level SDL3 wrapper (context, events, colors)
├── time/          Clock, delta time, useInterval, useTimer
└── ui/            Flex-based layout (View, Button, Slider, Knob, TextView, SpriteView)
```
