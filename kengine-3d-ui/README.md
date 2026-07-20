# Kengine 3D UI

GPU-backed UI primitives for `SDL_GPU_3D` applications.

`kengine-3d-ui` is separate from the existing Kengine UI stack because the current UI components are SDL renderer-backed, while 3D windows are owned by `SDL_GPU`. This module keeps the same simple retained-view idea, but renders with GPU textures and screen-space quads inside the active 3D render pass.

## Current Scope

- Retained `GpuUiView3D` trees with row/column layout.
- Labels, buttons, and sliders.
- Hover, click, release, and drag-focus handling.
- `GpuUiRenderer3D` for solid rectangles and SDL_ttf-backed text textures.

## Integration

Create a `GpuUiContext3D`, add root views, call `performLayout()`, and pass mouse events through `handleMouse(...)`. During rendering, draw the UI after the scene:

```kotlin
render(background.r, background.g, background.b, background.a, enableDepth = true) { frame ->
    sceneRenderer.draw(scene, frame, camera)
    uiRenderer.render(ui, frame)
}
```

Preload dynamic text before the render pass when labels change frequently. Texture uploads happen outside render passes, so callers should avoid creating new text textures while a pass is active.

## Next

- Move common shader/pipeline setup onto the shared `kengine-3d` shader/pipeline helpers.
- Add checkboxes, segmented controls, dropdowns, and scrollable panels.
- Add richer model-viewer controls for material/texture diagnostics.
