// src/main/cpp/box2d_wrapper.cpp

#include "box2d_wrapper.h"
#include <Box2D/Box2D.h>
#include <stdlib.h> // For NULL

extern "C" {

// Forward declaration of the world structure
struct Box2DWorld {
    b2World* world;
};

// Forward declaration of the body structure
struct Box2DBody {
    b2Body* body;
};

// World management

Box2DWorld* box2d_world_create(float gravity_x, float gravity_y) {
    Box2DWorld* bw = new Box2DWorld();
    bw->world = new b2World(b2Vec2(gravity_x, gravity_y));
    return bw;
}

void box2d_world_destroy(Box2DWorld* bw) {
    if (bw) {
        delete bw->world;
        delete bw;
    }
}

void box2d_world_step(Box2DWorld* bw, float timeStep, int velocityIterations, int positionIterations) {
    if (bw && bw->world) {
        bw->world->Step(timeStep, velocityIterations, positionIterations);
    }
}

// Body management

Box2DBodyPtr box2d_world_create_static_body(Box2DWorld* bw, float pos_x, float pos_y, float width, float height) {
    if (!bw || !bw->world) return 0;

    b2BodyDef bodyDef;
    bodyDef.type = b2_staticBody;
    bodyDef.position.Set(pos_x, pos_y);
    b2Body* body = bw->world->CreateBody(&bodyDef);

    b2PolygonShape box;
    box.SetAsBox(width / 2.0f, height / 2.0f);

    b2FixtureDef fixtureDef;
    fixtureDef.shape = &box;
    fixtureDef.density = 1.0f; // Density doesn't affect static bodies
    fixtureDef.friction = 0.3f;

    body->CreateFixture(&fixtureDef);

    // Allocate and store the body pointer
    Box2DBody* b = new Box2DBody();
    b->body = body;

    return reinterpret_cast<Box2DBodyPtr>(b);
}

Box2DBodyPtr box2d_world_create_dynamic_body(Box2DWorld* bw, float pos_x, float pos_y, float width, float height) {
    if (!bw || !bw->world) return 0;

    b2BodyDef bodyDef;
    bodyDef.type = b2_dynamicBody;
    bodyDef.position.Set(pos_x, pos_y);
    b2Body* body = bw->world->CreateBody(&bodyDef);

    b2PolygonShape box;
    box.SetAsBox(width / 2.0f, height / 2.0f);

    b2FixtureDef fixtureDef;
    fixtureDef.shape = &box;
    fixtureDef.density = 1.0f;
    fixtureDef.friction = 0.3f;

    body->CreateFixture(&fixtureDef);

    // Allocate and store the body pointer
    Box2DBody* b = new Box2DBody();
    b->body = body;

    return reinterpret_cast<Box2DBodyPtr>(b);
}

void box2d_body_set_position(Box2DBodyPtr body_ptr, float pos_x, float pos_y) {
    Box2DBody* b = reinterpret_cast<Box2DBody*>(body_ptr);
    if (b && b->body) {
        b->body->SetTransform(b2Vec2(pos_x, pos_y), b->body->GetAngle());
    }
}

void box2d_body_get_position(Box2DBodyPtr body_ptr, float* out_x, float* out_y) {
    Box2DBody* b = reinterpret_cast<Box2DBody*>(body_ptr);
    if (b && b->body && out_x && out_y) {
        b2Vec2 pos = b->body->GetPosition();
        *out_x = pos.x;
        *out_y = pos.y;
    }
}

void box2d_body_apply_force(Box2DBodyPtr body_ptr, float force_x, float force_y) {
    Box2DBody* b = reinterpret_cast<Box2DBody*>(body_ptr);
    if (b && b->body) {
        b->body->ApplyForceToCenter(b2Vec2(force_x, force_y), true);
    }
}

// Additional wrapper functions

// Function to create a circle shape fixture for a body
int32_t box2d_body_create_circle_fixture(Box2DBodyPtr body_ptr, float radius, float density, float friction) {
    Box2DBody* b = reinterpret_cast<Box2DBody*>(body_ptr);
    if (!b || !b->body) return -1;

    b2CircleShape circle;
    circle.m_radius = radius;

    b2FixtureDef fixtureDef;
    fixtureDef.shape = &circle;
    fixtureDef.density = density;
    fixtureDef.friction = friction;

    b->body->CreateFixture(&fixtureDef);
    return 0;
}

// Function to destroy a body
void box2d_world_destroy_body(Box2DWorld* bw, Box2DBodyPtr body_ptr) {
    Box2DBody* b = reinterpret_cast<Box2DBody*>(body_ptr);
    if (bw && bw->world && b && b->body) {
        bw->world->DestroyBody(b->body);
        delete b;
    }
}

// Function to get velocity of a body
void box2d_body_get_velocity(Box2DBodyPtr body_ptr, float* out_vel_x, float* out_vel_y) {
    Box2DBody* b = reinterpret_cast<Box2DBody*>(body_ptr);
    if (b && b->body && out_vel_x && out_vel_y) {
        b2Vec2 vel = b->body->GetLinearVelocity();
        *out_vel_x = vel.x;
        *out_vel_y = vel.y;
    }
}

// Function to set velocity of a body
void box2d_body_set_velocity(Box2DBodyPtr body_ptr, float vel_x, float vel_y) {
    Box2DBody* b = reinterpret_cast<Box2DBody*>(body_ptr);
    if (b && b->body) {
        b->body->SetLinearVelocity(b2Vec2(vel_x, vel_y));
    }
}

// Function to create a polygon shape fixture for a body
int32_t box2d_body_create_polygon_fixture(Box2DBodyPtr body_ptr, float vertices[][2], int32_t vertex_count, float density, float friction) {
    Box2DBody* b = reinterpret_cast<Box2DBody*>(body_ptr);
    if (!b || !b->body || vertex_count < 3) return -1;

    b2PolygonShape polygon;
    b2Vec2* b2_vertices = new b2Vec2[vertex_count];
    for (int32_t i = 0; i < vertex_count; ++i) {
        b2_vertices[i].Set(vertices[i][0], vertices[i][1]);
    }
    polygon.Set(b2_vertices, vertex_count);
    delete[] b2_vertices;

    b2FixtureDef fixtureDef;
    fixtureDef.shape = &polygon;
    fixtureDef.density = density;
    fixtureDef.friction = friction;

    b->body->CreateFixture(&fixtureDef);
    return 0;
}

} // extern "C"