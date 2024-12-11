// src/main/cpp/include/box2d_wrapper.h

#ifndef BOX2D_WRAPPER_H
#define BOX2D_WRAPPER_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef uintptr_t Box2DBodyPtr;

// World management
struct Box2DWorld;
struct Box2DWorld* box2d_world_create(float gravity_x, float gravity_y);
void box2d_world_destroy(struct Box2DWorld* bw);
void box2d_world_step(struct Box2DWorld* bw, float timeStep, int velocityIterations, int positionIterations);

// Body management
Box2DBodyPtr box2d_world_create_static_body(struct Box2DWorld* bw, float pos_x, float pos_y, float width, float height);
Box2DBodyPtr box2d_world_create_dynamic_body(struct Box2DWorld* bw, float pos_x, float pos_y, float width, float height);
void box2d_body_set_position(Box2DBodyPtr body_ptr, float pos_x, float pos_y);
void box2d_body_get_position(Box2DBodyPtr body_ptr, float* out_x, float* out_y);
void box2d_body_apply_force(Box2DBodyPtr body_ptr, float force_x, float force_y);

#ifdef __cplusplus
}
#endif

#endif // BOX2D_WRAPPER_H