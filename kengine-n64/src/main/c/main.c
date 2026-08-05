#include <stdio.h>
#include <string.h>
#include <libdragon.h>

#ifndef KENGINE_N64_C_ONLY
#include "kengine_n64_kotlin_api.h"
#endif
#ifdef KENGINE_N64_SPRITE_ASSETS
#include "kengine_n64_sprite_assets.h"
#endif
#ifdef KENGINE_N64_SOUND_ASSETS
#include "kengine_n64_sound_assets.h"
#endif

#define FB_WIDTH 320
#define FB_HEIGHT 240

#define KENGINE_INPUT_LEFT 1
#define KENGINE_INPUT_RIGHT (1 << 1)
#define KENGINE_INPUT_UP (1 << 2)
#define KENGINE_INPUT_DOWN (1 << 3)
#define KENGINE_INPUT_A (1 << 4)
#define KENGINE_INPUT_B (1 << 5)
#define KENGINE_INPUT_START (1 << 6)
#define KENGINE_INPUT_X (1 << 7)
#define KENGINE_INPUT_Y (1 << 8)
#define KENGINE_INPUT_L (1 << 9)
#define KENGINE_INPUT_R (1 << 10)
#define KENGINE_INPUT_SELECT (1 << 11)

#define KENGINE_RENDER_CLEAR 1
#define KENGINE_RENDER_FILL_RECT 2
#define KENGINE_RENDER_VERTICAL_GRADIENT 3
#define KENGINE_RENDER_DRAW_LINE 4
#define KENGINE_RENDER_DRAW_SPRITE 5
#define KENGINE_RENDER_DRAW_TEXT 6

#define KENGINE_RENDER_FIELD_TYPE 0
#define KENGINE_RENDER_FIELD_X 1
#define KENGINE_RENDER_FIELD_Y 2
#define KENGINE_RENDER_FIELD_WIDTH 3
#define KENGINE_RENDER_FIELD_HEIGHT 4
#define KENGINE_RENDER_FIELD_COLOR 5
#define KENGINE_RENDER_FIELD_COLOR2 6
#define KENGINE_RENDER_FIELD_PARAM 7
#define KENGINE_RENDER_FIELD_COUNT 8
#define KENGINE_RENDER_MAX_COMMANDS 256

#define KENGINE_AUDIO_LOOP_MUSIC 1
#define KENGINE_AUDIO_STOP_MUSIC 2
#define KENGINE_AUDIO_PLAY_SOUND 3

#define KENGINE_AUDIO_FIELD_TYPE 0
#define KENGINE_AUDIO_FIELD_ASSET_ID 1
#define KENGINE_AUDIO_FIELD_VOLUME 2
#define KENGINE_AUDIO_FIELD_PARAM 3
#define KENGINE_AUDIO_FIELD_COUNT 4
#define KENGINE_AUDIO_MAX_COMMANDS 16

#define KENGINE_N64_STORAGE_MAX_SLOTS 16
#define KENGINE_N64_STORAGE_KEY_MAX 32
#define KENGINE_N64_STORAGE_VALUE_MAX 512

#define STICK_DEADZONE 20

/* Storage: backed by in-memory slots (EEPROM/SRAM persistence TODO) */
typedef struct {
    char key[KENGINE_N64_STORAGE_KEY_MAX];
    unsigned char data[KENGINE_N64_STORAGE_VALUE_MAX];
    int size;
    int used;
} KengineN64StorageSlot;

static KengineN64StorageSlot g_storage_slots[KENGINE_N64_STORAGE_MAX_SLOTS];

static int storage_find_slot(const char* key) {
    for (int i = 0; i < KENGINE_N64_STORAGE_MAX_SLOTS; ++i) {
        if (g_storage_slots[i].used && strncmp(g_storage_slots[i].key, key, KENGINE_N64_STORAGE_KEY_MAX) == 0) {
            return i;
        }
    }
    return -1;
}

static int storage_find_free_slot(void) {
    for (int i = 0; i < KENGINE_N64_STORAGE_MAX_SLOTS; ++i) {
        if (!g_storage_slots[i].used) {
            return i;
        }
    }
    return -1;
}

int kengine_n64_storage_load(const char* key, void* destination, int max_bytes) {
    if (!key || !destination || max_bytes <= 0) return -1;

    int slot = storage_find_slot(key);
    if (slot < 0) return -1;

    int copy_size = g_storage_slots[slot].size;
    if (copy_size > max_bytes) copy_size = max_bytes;

    memcpy(destination, g_storage_slots[slot].data, copy_size);
    return copy_size;
}

int kengine_n64_storage_save(const char* key, const void* data, int size) {
    if (!key) return 0;
    if (size < 0 || size > KENGINE_N64_STORAGE_VALUE_MAX) return 0;

    int slot = storage_find_slot(key);
    if (slot < 0) {
        slot = storage_find_free_slot();
        if (slot < 0) return 0;
    }

    strncpy(g_storage_slots[slot].key, key, KENGINE_N64_STORAGE_KEY_MAX - 1);
    g_storage_slots[slot].key[KENGINE_N64_STORAGE_KEY_MAX - 1] = '\0';

    if (data && size > 0) {
        memcpy(g_storage_slots[slot].data, data, size);
    }
    g_storage_slots[slot].size = size;
    g_storage_slots[slot].used = 1;

    return 1;
}

int kengine_n64_storage_delete(const char* key) {
    if (!key) return 0;

    int slot = storage_find_slot(key);
    if (slot < 0) return 0;

    g_storage_slots[slot].used = 0;
    g_storage_slots[slot].key[0] = '\0';
    g_storage_slots[slot].size = 0;

    return 1;
}

int kengine_n64_storage_exists(const char* key) {
    if (!key) return 0;
    return storage_find_slot(key) >= 0 ? 1 : 0;
}

/* 5x7 bitmap font (printable ASCII 32-127) */
static const unsigned char kengine_font[96][7] = {
    {0x00,0x00,0x00,0x00,0x00,0x00,0x00}, /* space */
    {0x04,0x04,0x04,0x04,0x04,0x00,0x04}, /* ! */
    {0x0A,0x0A,0x00,0x00,0x00,0x00,0x00}, /* " */
    {0x0A,0x1F,0x0A,0x0A,0x1F,0x0A,0x00}, /* # */
    {0x04,0x0F,0x14,0x0E,0x05,0x1E,0x04}, /* $ */
    {0x18,0x19,0x02,0x04,0x08,0x13,0x03}, /* % */
    {0x08,0x14,0x14,0x08,0x15,0x12,0x0D}, /* & */
    {0x04,0x04,0x00,0x00,0x00,0x00,0x00}, /* ' */
    {0x02,0x04,0x08,0x08,0x08,0x04,0x02}, /* ( */
    {0x08,0x04,0x02,0x02,0x02,0x04,0x08}, /* ) */
    {0x00,0x04,0x15,0x0E,0x15,0x04,0x00}, /* * */
    {0x00,0x04,0x04,0x1F,0x04,0x04,0x00}, /* + */
    {0x00,0x00,0x00,0x00,0x00,0x04,0x08}, /* , */
    {0x00,0x00,0x00,0x1F,0x00,0x00,0x00}, /* - */
    {0x00,0x00,0x00,0x00,0x00,0x00,0x04}, /* . */
    {0x01,0x02,0x02,0x04,0x08,0x08,0x10}, /* / */
    {0x0E,0x11,0x13,0x15,0x19,0x11,0x0E}, /* 0 */
    {0x04,0x0C,0x04,0x04,0x04,0x04,0x0E}, /* 1 */
    {0x0E,0x11,0x01,0x06,0x08,0x10,0x1F}, /* 2 */
    {0x0E,0x11,0x01,0x06,0x01,0x11,0x0E}, /* 3 */
    {0x02,0x06,0x0A,0x12,0x1F,0x02,0x02}, /* 4 */
    {0x1F,0x10,0x1E,0x01,0x01,0x11,0x0E}, /* 5 */
    {0x06,0x08,0x10,0x1E,0x11,0x11,0x0E}, /* 6 */
    {0x1F,0x01,0x02,0x04,0x08,0x08,0x08}, /* 7 */
    {0x0E,0x11,0x11,0x0E,0x11,0x11,0x0E}, /* 8 */
    {0x0E,0x11,0x11,0x0F,0x01,0x02,0x0C}, /* 9 */
    {0x00,0x00,0x04,0x00,0x00,0x04,0x00}, /* : */
    {0x00,0x00,0x04,0x00,0x00,0x04,0x08}, /* ; */
    {0x02,0x04,0x08,0x10,0x08,0x04,0x02}, /* < */
    {0x00,0x00,0x1F,0x00,0x1F,0x00,0x00}, /* = */
    {0x08,0x04,0x02,0x01,0x02,0x04,0x08}, /* > */
    {0x0E,0x11,0x01,0x06,0x04,0x00,0x04}, /* ? */
    {0x0E,0x11,0x17,0x15,0x17,0x10,0x0E}, /* @ */
    {0x0E,0x11,0x11,0x1F,0x11,0x11,0x11}, /* A */
    {0x1E,0x11,0x11,0x1E,0x11,0x11,0x1E}, /* B */
    {0x0E,0x11,0x10,0x10,0x10,0x11,0x0E}, /* C */
    {0x1E,0x11,0x11,0x11,0x11,0x11,0x1E}, /* D */
    {0x1F,0x10,0x10,0x1E,0x10,0x10,0x1F}, /* E */
    {0x1F,0x10,0x10,0x1E,0x10,0x10,0x10}, /* F */
    {0x0E,0x11,0x10,0x17,0x11,0x11,0x0F}, /* G */
    {0x11,0x11,0x11,0x1F,0x11,0x11,0x11}, /* H */
    {0x0E,0x04,0x04,0x04,0x04,0x04,0x0E}, /* I */
    {0x07,0x02,0x02,0x02,0x02,0x12,0x0C}, /* J */
    {0x11,0x12,0x14,0x18,0x14,0x12,0x11}, /* K */
    {0x10,0x10,0x10,0x10,0x10,0x10,0x1F}, /* L */
    {0x11,0x1B,0x15,0x15,0x11,0x11,0x11}, /* M */
    {0x11,0x19,0x15,0x13,0x11,0x11,0x11}, /* N */
    {0x0E,0x11,0x11,0x11,0x11,0x11,0x0E}, /* O */
    {0x1E,0x11,0x11,0x1E,0x10,0x10,0x10}, /* P */
    {0x0E,0x11,0x11,0x11,0x15,0x12,0x0D}, /* Q */
    {0x1E,0x11,0x11,0x1E,0x14,0x12,0x11}, /* R */
    {0x0E,0x11,0x10,0x0E,0x01,0x11,0x0E}, /* S */
    {0x1F,0x04,0x04,0x04,0x04,0x04,0x04}, /* T */
    {0x11,0x11,0x11,0x11,0x11,0x11,0x0E}, /* U */
    {0x11,0x11,0x11,0x11,0x0A,0x0A,0x04}, /* V */
    {0x11,0x11,0x11,0x15,0x15,0x1B,0x11}, /* W */
    {0x11,0x11,0x0A,0x04,0x0A,0x11,0x11}, /* X */
    {0x11,0x11,0x0A,0x04,0x04,0x04,0x04}, /* Y */
    {0x1F,0x01,0x02,0x04,0x08,0x10,0x1F}, /* Z */
    {0x0E,0x08,0x08,0x08,0x08,0x08,0x0E}, /* [ */
    {0x10,0x08,0x08,0x04,0x02,0x02,0x01}, /* backslash */
    {0x0E,0x02,0x02,0x02,0x02,0x02,0x0E}, /* ] */
    {0x04,0x0A,0x11,0x00,0x00,0x00,0x00}, /* ^ */
    {0x00,0x00,0x00,0x00,0x00,0x00,0x1F}, /* _ */
    {0x08,0x04,0x00,0x00,0x00,0x00,0x00}, /* ` */
    {0x00,0x00,0x0E,0x01,0x0F,0x11,0x0F}, /* a */
    {0x10,0x10,0x1E,0x11,0x11,0x11,0x1E}, /* b */
    {0x00,0x00,0x0E,0x11,0x10,0x11,0x0E}, /* c */
    {0x01,0x01,0x0F,0x11,0x11,0x11,0x0F}, /* d */
    {0x00,0x00,0x0E,0x11,0x1F,0x10,0x0E}, /* e */
    {0x06,0x08,0x1E,0x08,0x08,0x08,0x08}, /* f */
    {0x00,0x00,0x0F,0x11,0x0F,0x01,0x0E}, /* g */
    {0x10,0x10,0x1E,0x11,0x11,0x11,0x11}, /* h */
    {0x04,0x00,0x0C,0x04,0x04,0x04,0x0E}, /* i */
    {0x02,0x00,0x06,0x02,0x02,0x12,0x0C}, /* j */
    {0x10,0x10,0x12,0x14,0x18,0x14,0x12}, /* k */
    {0x0C,0x04,0x04,0x04,0x04,0x04,0x0E}, /* l */
    {0x00,0x00,0x1A,0x15,0x15,0x11,0x11}, /* m */
    {0x00,0x00,0x1E,0x11,0x11,0x11,0x11}, /* n */
    {0x00,0x00,0x0E,0x11,0x11,0x11,0x0E}, /* o */
    {0x00,0x00,0x1E,0x11,0x1E,0x10,0x10}, /* p */
    {0x00,0x00,0x0F,0x11,0x0F,0x01,0x01}, /* q */
    {0x00,0x00,0x16,0x19,0x10,0x10,0x10}, /* r */
    {0x00,0x00,0x0F,0x10,0x0E,0x01,0x1E}, /* s */
    {0x08,0x08,0x1E,0x08,0x08,0x09,0x06}, /* t */
    {0x00,0x00,0x11,0x11,0x11,0x11,0x0F}, /* u */
    {0x00,0x00,0x11,0x11,0x11,0x0A,0x04}, /* v */
    {0x00,0x00,0x11,0x11,0x15,0x15,0x0A}, /* w */
    {0x00,0x00,0x11,0x0A,0x04,0x0A,0x11}, /* x */
    {0x00,0x00,0x11,0x11,0x0F,0x01,0x0E}, /* y */
    {0x00,0x00,0x1F,0x02,0x04,0x08,0x1F}, /* z */
    {0x02,0x04,0x04,0x08,0x04,0x04,0x02}, /* { */
    {0x04,0x04,0x04,0x04,0x04,0x04,0x04}, /* | */
    {0x08,0x04,0x04,0x02,0x04,0x04,0x08}, /* } */
    {0x00,0x00,0x08,0x15,0x02,0x00,0x00}, /* ~ */
    {0x00,0x00,0x00,0x00,0x00,0x00,0x00}, /* DEL */
};

static uint32_t kengine_rgba_to_color(int rgba) {
    uint8_t r = (rgba >> 24) & 0xFF;
    uint8_t g = (rgba >> 16) & 0xFF;
    uint8_t b = (rgba >> 8) & 0xFF;
    return graphics_make_color(r, g, b, 0xFF);
}

static void draw_rect(surface_t* disp, int x, int y, int width, int height, int color) {
    uint32_t c = kengine_rgba_to_color(color);

    int x0 = x < 0 ? 0 : x;
    int y0 = y < 0 ? 0 : y;
    int x1 = x + width;
    int y1 = y + height;
    if (x1 > FB_WIDTH) x1 = FB_WIDTH;
    if (y1 > FB_HEIGHT) y1 = FB_HEIGHT;

    if (x0 >= x1 || y0 >= y1) return;

    graphics_draw_box(disp, x0, y0, x1 - x0, y1 - y0, c);
}

static void draw_line(surface_t* disp, int x0, int y0, int x1, int y1, int color) {
    uint32_t c = kengine_rgba_to_color(color);
    graphics_draw_line(disp, x0, y0, x1, y1, c);
}

static void draw_text(surface_t* disp, const char* text, int x, int y, uint32_t color, int scale) {
    if (!text) return;
    if (scale < 1) scale = 1;

    int cursor_x = x;
    int cursor_y = y;

    for (const char* p = text; *p; ++p) {
        unsigned char ch = (unsigned char)*p;
        if (ch == '\n') {
            cursor_x = x;
            cursor_y += 8 * scale;
            continue;
        }
        if (ch < 32 || ch > 127) ch = '?';

        const unsigned char* glyph = kengine_font[ch - 32];

        for (int row = 0; row < 7; ++row) {
            unsigned char bits = glyph[row];
            for (int col = 0; col < 5; ++col) {
                if (bits & (0x10 >> col)) {
                    int px = cursor_x + col * scale;
                    int py = cursor_y + row * scale;
                    if (scale == 1) {
                        if (px >= 0 && px < FB_WIDTH && py >= 0 && py < FB_HEIGHT) {
                            graphics_draw_pixel(disp, px, py, color);
                        }
                    } else {
                        graphics_draw_box(disp, px, py, scale, scale, color);
                    }
                }
            }
        }
        cursor_x += 6 * scale;
    }
}

static void draw_text_rgba(surface_t* disp, const char* text, int x, int y, int color, int scale) {
    draw_text(disp, text, x, y, kengine_rgba_to_color(color), scale);
}

static void draw_vertical_gradient(surface_t* disp, int top_color, int bottom_color, int pulse) {
    (void)pulse;

    uint8_t tr = (top_color >> 24) & 0xFF;
    uint8_t tg = (top_color >> 16) & 0xFF;
    uint8_t tb = (top_color >> 8) & 0xFF;
    uint8_t br = (bottom_color >> 24) & 0xFF;
    uint8_t bg = (bottom_color >> 16) & 0xFF;
    uint8_t bb = (bottom_color >> 8) & 0xFF;

    for (int y = 0; y < FB_HEIGHT; ++y) {
        uint8_t r = tr + (br - tr) * y / FB_HEIGHT;
        uint8_t g = tg + (bg - tg) * y / FB_HEIGHT;
        uint8_t b = tb + (bb - tb) * y / FB_HEIGHT;
        uint32_t c = graphics_make_color(r, g, b, 0xFF);
        graphics_draw_line(disp, 0, y, FB_WIDTH - 1, y, c);
    }
}

#ifdef KENGINE_N64_SPRITE_ASSETS
static void draw_sprite(surface_t* disp, int sprite_id, int x, int y, int width, int height, int tint, int frame) {
    (void)tint;

    const KengineN64SpriteAsset* asset = kengine_n64_find_sprite_asset(sprite_id);
    if (!asset) {
        draw_rect(disp, x, y, width > 0 ? width : 16, height > 0 ? height : 16, 0xFF00FFFF);
        return;
    }

    int tile_w = asset->tile_width > 0 ? asset->tile_width : asset->width;
    int tile_h = asset->tile_height > 0 ? asset->tile_height : asset->height;
    int columns = asset->columns > 0 ? asset->columns : (asset->width / tile_w);
    if (columns < 1) columns = 1;

    int tile_col = frame % columns;
    int tile_row = frame / columns;
    int src_x = tile_col * tile_w;
    int src_y = tile_row * tile_h;
    int src_stride = asset->width;

    const unsigned char* src = asset->data_start;
    size_t data_size = (size_t)(asset->data_end - asset->data_start);
    int total_pixels = (int)(data_size / 4);

    int draw_w = width > 0 ? width : tile_w;
    int draw_h = height > 0 ? height : tile_h;

    for (int dy = 0; dy < draw_h && dy < tile_h; ++dy) {
        for (int dx = 0; dx < draw_w && dx < tile_w; ++dx) {
            int src_pixel = (src_y + dy) * src_stride + (src_x + dx);
            if (src_pixel < 0 || src_pixel >= total_pixels) continue;

            int offset = src_pixel * 4;
            uint8_t r = src[offset];
            uint8_t g = src[offset + 1];
            uint8_t b = src[offset + 2];
            uint8_t a = src[offset + 3];
            if (a < 128) continue;

            int px = x + dx;
            int py = y + dy;
            if (px < 0 || px >= FB_WIDTH || py < 0 || py >= FB_HEIGHT) continue;

            uint32_t c = graphics_make_color(r, g, b, 0xFF);
            graphics_draw_pixel(disp, px, py, c);
        }
    }
}
#endif

static int g_render_commands[KENGINE_RENDER_MAX_COMMANDS * KENGINE_RENDER_FIELD_COUNT];
static int g_audio_commands[KENGINE_AUDIO_MAX_COMMANDS * KENGINE_AUDIO_FIELD_COUNT];

static int translate_input(joypad_inputs_t inputs, joypad_buttons_t held) {
    int mask = 0;

    if (held.d_left || inputs.stick_x < -STICK_DEADZONE) mask |= KENGINE_INPUT_LEFT;
    if (held.d_right || inputs.stick_x > STICK_DEADZONE) mask |= KENGINE_INPUT_RIGHT;
    if (held.d_up || inputs.stick_y > STICK_DEADZONE) mask |= KENGINE_INPUT_UP;
    if (held.d_down || inputs.stick_y < -STICK_DEADZONE) mask |= KENGINE_INPUT_DOWN;
    if (held.a) mask |= KENGINE_INPUT_A;
    if (held.b) mask |= KENGINE_INPUT_B;
    if (held.start) mask |= KENGINE_INPUT_START;
    if (held.c_up) mask |= KENGINE_INPUT_X;
    if (held.c_down) mask |= KENGINE_INPUT_Y;
    if (held.l) mask |= KENGINE_INPUT_L;
    if (held.r) mask |= KENGINE_INPUT_R;
    if (held.z) mask |= KENGINE_INPUT_SELECT;

    return mask;
}

static void execute_render_commands(surface_t* disp, int* commands, int command_count) {
    for (int i = 0; i < command_count; ++i) {
        int base = i * KENGINE_RENDER_FIELD_COUNT;
        int type = commands[base + KENGINE_RENDER_FIELD_TYPE];
        int x = commands[base + KENGINE_RENDER_FIELD_X];
        int y = commands[base + KENGINE_RENDER_FIELD_Y];
        int w = commands[base + KENGINE_RENDER_FIELD_WIDTH];
        int h = commands[base + KENGINE_RENDER_FIELD_HEIGHT];
        int color = commands[base + KENGINE_RENDER_FIELD_COLOR];
        int color2 = commands[base + KENGINE_RENDER_FIELD_COLOR2];
        int param = commands[base + KENGINE_RENDER_FIELD_PARAM];

        switch (type) {
            case KENGINE_RENDER_CLEAR: {
                uint32_t c = kengine_rgba_to_color(color);
                graphics_fill_screen(disp, c);
                break;
            }
            case KENGINE_RENDER_FILL_RECT:
                draw_rect(disp, x, y, w, h, color);
                break;
            case KENGINE_RENDER_VERTICAL_GRADIENT:
                draw_vertical_gradient(disp, color, color2, param);
                break;
            case KENGINE_RENDER_DRAW_LINE:
                draw_line(disp, x, y, w, h, color);
                break;
            case KENGINE_RENDER_DRAW_SPRITE:
#ifdef KENGINE_N64_SPRITE_ASSETS
                draw_sprite(disp, color2, x, y, w, h, color, param);
#else
                draw_rect(disp, x, y, w > 0 ? w : 16, h > 0 ? h : 16, 0xFF00FFFF);
#endif
                break;
            case KENGINE_RENDER_DRAW_TEXT: {
#ifndef KENGINE_N64_C_ONLY
                const char* text = (const char*)kengine_n64_kotlin_kengineN64RuntimeCommandText(i);
                int scale = param > 0 ? param : 1;
                draw_text_rgba(disp, text, x, y, color, scale);
#endif
                break;
            }
            default:
                break;
        }
    }
}

#ifdef KENGINE_N64_C_ONLY

int main(void) {
    display_init(RESOLUTION_320x240, DEPTH_16_BPP, 3, GAMMA_NONE, FILTERS_RESAMPLE);
    joypad_init();

    int frame = 0;
    int square_x = 120;
    int square_y = 80;
    int square_size = 40;

    uint32_t bg_color = graphics_make_color(0x10, 0x10, 0x30, 0xFF);
    uint32_t fg_color = graphics_make_color(0x00, 0xFF, 0x00, 0xFF);
    uint32_t text_color = graphics_make_color(0xFF, 0xFF, 0xFF, 0xFF);
    uint32_t dim_color = graphics_make_color(0xAA, 0xAA, 0xAA, 0xFF);
    uint32_t border_color = graphics_make_color(0x44, 0x44, 0x88, 0xFF);

    while (1) {
        surface_t* disp = display_get();

        joypad_poll();
        joypad_inputs_t inputs = joypad_get_inputs(JOYPAD_PORT_1);
        joypad_buttons_t held = joypad_get_buttons_held(JOYPAD_PORT_1);
        joypad_buttons_t pressed = joypad_get_buttons_pressed(JOYPAD_PORT_1);

        int dx = 0, dy = 0;
        if (held.d_left || inputs.stick_x < -STICK_DEADZONE) dx = -2;
        if (held.d_right || inputs.stick_x > STICK_DEADZONE) dx = 2;
        if (held.d_up || inputs.stick_y > STICK_DEADZONE) dy = -2;
        if (held.d_down || inputs.stick_y < -STICK_DEADZONE) dy = 2;

        square_x += dx;
        square_y += dy;

        if (square_x < 0) square_x = 0;
        if (square_y < 0) square_y = 0;
        if (square_x + square_size > FB_WIDTH) square_x = FB_WIDTH - square_size;
        if (square_y + square_size > FB_HEIGHT) square_y = FB_HEIGHT - square_size;

        if (pressed.a) {
            fg_color = graphics_make_color(
                (frame * 73) & 0xFF,
                (frame * 137 + 100) & 0xFF,
                (frame * 53 + 200) & 0xFF,
                0xFF
            );
        }

        if (pressed.b) {
            square_size = (square_size < 48) ? square_size + 4 : 8;
        }

        graphics_fill_screen(disp, bg_color);

        /* Border */
        graphics_draw_box(disp, 0, 0, FB_WIDTH, 2, border_color);
        graphics_draw_box(disp, 0, FB_HEIGHT - 2, FB_WIDTH, 2, border_color);
        graphics_draw_box(disp, 0, 0, 2, FB_HEIGHT, border_color);
        graphics_draw_box(disp, FB_WIDTH - 2, 0, 2, FB_HEIGHT, border_color);

        /* Square */
        graphics_draw_box(disp, square_x, square_y, square_size, square_size, fg_color);

        /* Text */
        draw_text(disp, "Kengine N64", 80, 8, text_color, 2);
        draw_text(disp, "D-Pad: move  A: color  B: size", 10, 226, dim_color, 1);

        char buf[64];
        snprintf(buf, sizeof(buf), "Frame: %d  Pos: %d,%d", frame, square_x, square_y);
        draw_text(disp, buf, 10, 30, dim_color, 1);

        display_show(disp);
        frame++;
    }

    display_close();
    return 0;
}

#else

static void kotlin_runtime_start(void) {
    kengine_n64_kotlin_kengineN64RuntimeStart();
}

static int kotlin_runtime_update(int host_frame, int input_mask) {
    return kengine_n64_kotlin_kengineN64RuntimeUpdate(host_frame, input_mask);
}

static int kotlin_runtime_audio(int host_frame) {
    return kengine_n64_kotlin_kengineN64RuntimeAudio(host_frame);
}

static int kotlin_runtime_draw(int host_frame) {
    return kengine_n64_kotlin_kengineN64RuntimeDraw(host_frame, FB_WIDTH, FB_HEIGHT);
}

static int kotlin_copy_commands(int* destination, int max_commands) {
    return kengine_n64_kotlin_kengineN64RuntimeCopyCommands(destination, max_commands);
}

static int kotlin_copy_audio_commands(int* destination, int max_commands) {
    return kengine_n64_kotlin_kengineN64RuntimeCopyAudioCommands(destination, max_commands);
}

int main(void) {
    display_init(RESOLUTION_320x240, DEPTH_16_BPP, 3, GAMMA_NONE, FILTERS_RESAMPLE);
    joypad_init();

    memset(g_storage_slots, 0, sizeof(g_storage_slots));

    {
        surface_t* d = display_get();
        graphics_fill_screen(d, graphics_make_color(0x00, 0x00, 0x80, 0xFF));
        draw_text(d, "DBG1: pre-init", 10, 10, graphics_make_color(0xFF, 0xFF, 0x00, 0xFF), 1);
        display_show(d);
    }

    disable_interrupts();
    kotlin_runtime_start();
    enable_interrupts();

    {
        surface_t* d = display_get();
        graphics_fill_screen(d, graphics_make_color(0x00, 0x80, 0x00, 0xFF));
        draw_text(d, "DBG2: post-init OK!", 10, 10, graphics_make_color(0xFF, 0xFF, 0xFF, 0xFF), 1);
        display_show(d);
    }

    int frame = 0;

    while (1) {
        surface_t* disp = display_get();

        joypad_poll();
        joypad_inputs_t inputs = joypad_get_inputs(JOYPAD_PORT_1);
        joypad_buttons_t held = joypad_get_buttons_held(JOYPAD_PORT_1);
        int input_mask = translate_input(inputs, held);

        kotlin_runtime_update(frame, input_mask);
        kotlin_runtime_audio(frame);
        kotlin_runtime_draw(frame);

        int render_count = kotlin_copy_commands(g_render_commands, KENGINE_RENDER_MAX_COMMANDS);
        execute_render_commands(disp, g_render_commands, render_count);

        display_show(disp);
        frame++;
    }

    kengine_n64_kotlin_kengineN64RuntimeCleanup();
    return 0;
}

#endif
