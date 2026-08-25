#ifndef KENGINE_N64_WORLD_MESH_TYPES_H
#define KENGINE_N64_WORLD_MESH_TYPES_H

#include <stdint.h>

/*
 * Reusable static-world asset contract consumed by the N64 GL backend.
 * Game asset generators emit data conforming to these types; render behavior
 * stays in kengine-n64 rather than being hard-coded for a particular game.
 */
typedef enum {
    KENGINE_WORLD_TEXTURE_RGBA16 = 0,
    KENGINE_WORLD_TEXTURE_IA16 = 1
} KengineWorldTextureFormat;

typedef enum {
    KENGINE_WORLD_MATERIAL_OPAQUE = 0,
    KENGINE_WORLD_MATERIAL_MASKED = 1,
    KENGINE_WORLD_MATERIAL_TRANSLUCENT_DECAL = 2
} KengineWorldMaterialMode;

typedef struct {
    int material_index;
    int width;
    int height;
    int format;
    int material_mode;
    const uint16_t* data;
} KengineWorldTexture;

typedef struct {
    int mesh_id;
    int vertex_count;
    int triangle_count;
    int color_count;
    int vertex_stride;
    int texture_count;
    const int* vertices;
    const int* triangles;
    const int* colors;
    const KengineWorldTexture* textures;
} KengineWorldMesh;

#endif
