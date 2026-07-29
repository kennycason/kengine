#include <stdio.h>
#include <string.h>
#include <malloc.h>
#include <switch.h>

#ifndef KENGINE_SWITCH_C_ONLY
#include "kengine_switch_kotlin_api.h"
#endif

#define FB_WIDTH 1280
#define FB_HEIGHT 720

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
#define KENGINE_RENDER_MAX_COMMANDS 1024

#define KENGINE_AUDIO_LOOP_MUSIC 1
#define KENGINE_AUDIO_STOP_MUSIC 2

#define KENGINE_AUDIO_FIELD_TYPE 0
#define KENGINE_AUDIO_FIELD_ASSET_ID 1
#define KENGINE_AUDIO_FIELD_VOLUME 2
#define KENGINE_AUDIO_FIELD_PARAM 3
#define KENGINE_AUDIO_FIELD_COUNT 4
#define KENGINE_AUDIO_MAX_COMMANDS 32

#ifndef KENGINE_SWITCH_C_ONLY
static kengine_switch_kotlin_ExportedSymbols* kotlin_symbols(void) {
    return kengine_switch_kotlin_symbols();
}

static kengine_switch_kotlin_KInt g_render_commands[KENGINE_RENDER_MAX_COMMANDS * KENGINE_RENDER_FIELD_COUNT];
static kengine_switch_kotlin_KInt g_audio_commands[KENGINE_AUDIO_MAX_COMMANDS * KENGINE_AUDIO_FIELD_COUNT];
#endif

static int kotlin_add_probe(void) {
#ifdef KENGINE_SWITCH_C_ONLY
    return 42;
#else
    kengine_switch_kotlin_ExportedSymbols* symbols = kotlin_symbols();
    return symbols->kotlin.root.kengineSwitchAdd(20, 22);
#endif
}

#ifdef KENGINE_SWITCH_C_ONLY
static int run_console_smoke_test(void) {
    consoleInit(NULL);

    PadState pad;
    padConfigureInput(1, HidNpadStyleSet_NpadStandard);
    padInitializeDefault(&pad);

    printf("Kengine Nintendo Switch\n");
    printf("Hello from libnx.\n");
    printf("Kotlin add probe: %d\n", kotlin_add_probe());
    printf("Kotlin linkage disabled for this build.\n");
    printf("\nPress + to exit.\n");

    while (appletMainLoop()) {
        padUpdate(&pad);

        u64 buttons_down = padGetButtonsDown(&pad);
        if (buttons_down & HidNpadButton_Plus) {
            break;
        }

        consoleUpdate(NULL);
    }

    consoleExit(NULL);
    return 0;
}
#else
static int kotlin_message_code_probe(void) {
    kengine_switch_kotlin_ExportedSymbols* symbols = kotlin_symbols();
    return symbols->kotlin.root.kengineSwitchMessageCode();
}

static int kotlin_allocation_probe(int seed, int size) {
    kengine_switch_kotlin_ExportedSymbols* symbols = kotlin_symbols();
    return symbols->kotlin.root.kengineSwitchAllocationProbe(seed, size);
}

static void kotlin_dispose_string(const char* message) {
    if (message != NULL) {
        kotlin_symbols()->DisposeString(message);
    }
}

static int kotlin_startup_probe(void) {
    kengine_switch_kotlin_ExportedSymbols* symbols = kotlin_symbols();
    const int iterations = 8;
    int checksum = 0;

    for (int index = 0; index < iterations; ++index) {
        int add = symbols->kotlin.root.kengineSwitchAdd(index, index * 2);
        int allocation = symbols->kotlin.root.kengineSwitchAllocationProbe(index + 1, 24 + index);
        checksum ^= add + allocation + (index * 17);
    }

    const char* message = symbols->kotlin.root.kengineSwitchProbeMessage(iterations, checksum);
    kotlin_dispose_string(message);
    return checksum;
}

static void kotlin_runtime_start(void) {
    kengine_switch_kotlin_ExportedSymbols* symbols = kotlin_symbols();
    const char* message = symbols->kotlin.root.kengineSwitchRuntimeStart();
    kotlin_dispose_string(message);
}

static int kotlin_runtime_update(int host_frame, int input_mask) {
    kengine_switch_kotlin_ExportedSymbols* symbols = kotlin_symbols();
    return symbols->kotlin.root.kengineSwitchRuntimeUpdate(host_frame, input_mask);
}

static int kotlin_runtime_audio(int host_frame) {
    kengine_switch_kotlin_ExportedSymbols* symbols = kotlin_symbols();
    return symbols->kotlin.root.kengineSwitchRuntimeAudio(host_frame);
}

static int kotlin_runtime_draw(int host_frame) {
    kengine_switch_kotlin_ExportedSymbols* symbols = kotlin_symbols();
    return symbols->kotlin.root.kengineSwitchRuntimeDraw(host_frame, FB_WIDTH, FB_HEIGHT);
}

static void kotlin_runtime_snapshot(void) {
    kengine_switch_kotlin_ExportedSymbols* symbols = kotlin_symbols();
    const char* message = symbols->kotlin.root.kengineSwitchRuntimeSnapshot();
    kotlin_dispose_string(message);
}

static void kotlin_runtime_cleanup(void) {
    kengine_switch_kotlin_ExportedSymbols* symbols = kotlin_symbols();
    const char* message = symbols->kotlin.root.kengineSwitchRuntimeCleanup();
    kotlin_dispose_string(message);
}

static int kotlin_copy_commands(kengine_switch_kotlin_KInt* destination, int max_commands) {
    kengine_switch_kotlin_ExportedSymbols* symbols = kotlin_symbols();
    return symbols->kotlin.root.kengineSwitchRuntimeCopyCommands(destination, max_commands);
}

static int kotlin_copy_audio_commands(kengine_switch_kotlin_KInt* destination, int max_commands) {
    kengine_switch_kotlin_ExportedSymbols* symbols = kotlin_symbols();
    return symbols->kotlin.root.kengineSwitchRuntimeCopyAudioCommands(destination, max_commands);
}

static const char* kotlin_command_text(int command_index) {
    kengine_switch_kotlin_ExportedSymbols* symbols = kotlin_symbols();
    return symbols->kotlin.root.kengineSwitchRuntimeCommandText(command_index);
}

static int input_mask_from_buttons(u64 buttons) {
    int input_mask = 0;

    if (buttons & HidNpadButton_AnyLeft) {
        input_mask |= KENGINE_INPUT_LEFT;
    }
    if (buttons & HidNpadButton_AnyRight) {
        input_mask |= KENGINE_INPUT_RIGHT;
    }
    if (buttons & HidNpadButton_AnyUp) {
        input_mask |= KENGINE_INPUT_UP;
    }
    if (buttons & HidNpadButton_AnyDown) {
        input_mask |= KENGINE_INPUT_DOWN;
    }
    if (buttons & HidNpadButton_A) {
        input_mask |= KENGINE_INPUT_A;
    }
    if (buttons & HidNpadButton_B) {
        input_mask |= KENGINE_INPUT_B;
    }
    if (buttons & HidNpadButton_Plus) {
        input_mask |= KENGINE_INPUT_START;
    }
    if (buttons & HidNpadButton_X) {
        input_mask |= KENGINE_INPUT_X;
    }
    if (buttons & HidNpadButton_Y) {
        input_mask |= KENGINE_INPUT_Y;
    }
    if (buttons & (HidNpadButton_L | HidNpadButton_ZL)) {
        input_mask |= KENGINE_INPUT_L;
    }
    if (buttons & (HidNpadButton_R | HidNpadButton_ZR)) {
        input_mask |= KENGINE_INPUT_R;
    }
    if (buttons & HidNpadButton_Minus) {
        input_mask |= KENGINE_INPUT_SELECT;
    }

    return input_mask;
}

static int clamp_int(int value, int minimum, int maximum) {
    if (value < minimum) {
        return minimum;
    }
    if (value > maximum) {
        return maximum;
    }
    return value;
}

static u32 color_mix(u32 from, u32 to, int amount, int maximum) {
    amount = clamp_int(amount, 0, maximum);
    int inverse = maximum - amount;

    u32 r = (((from >> 0) & 0xff) * inverse + ((to >> 0) & 0xff) * amount) / maximum;
    u32 g = (((from >> 8) & 0xff) * inverse + ((to >> 8) & 0xff) * amount) / maximum;
    u32 b = (((from >> 16) & 0xff) * inverse + ((to >> 16) & 0xff) * amount) / maximum;
    return RGBA8_MAXALPHA(r, g, b);
}

static u32 color_add(u32 color, int amount) {
    u32 r = (u32)clamp_int((int)((color >> 0) & 0xff) + amount, 0, 255);
    u32 g = (u32)clamp_int((int)((color >> 8) & 0xff) + amount, 0, 255);
    u32 b = (u32)clamp_int((int)((color >> 16) & 0xff) + amount, 0, 255);
    return RGBA8_MAXALPHA(r, g, b);
}

static u32 color_tint(u32 color, u32 tint) {
    u32 r = (((color >> 0) & 0xff) * ((tint >> 0) & 0xff)) / 255;
    u32 g = (((color >> 8) & 0xff) * ((tint >> 8) & 0xff)) / 255;
    u32 b = (((color >> 16) & 0xff) * ((tint >> 16) & 0xff)) / 255;
    return RGBA8_MAXALPHA(r, g, b);
}

static void draw_rect(u32* framebuf, u32 stride_pixels, int x, int y, int width, int height, u32 color) {
    int left = clamp_int(x, 0, FB_WIDTH);
    int top = clamp_int(y, 0, FB_HEIGHT);
    int right = clamp_int(x + width, 0, FB_WIDTH);
    int bottom = clamp_int(y + height, 0, FB_HEIGHT);

    for (int py = top; py < bottom; ++py) {
        u32* row = framebuf + py * stride_pixels;
        for (int px = left; px < right; ++px) {
            row[px] = color;
        }
    }
}

static int abs_int(int value) {
    return value < 0 ? -value : value;
}

static void draw_pixel(u32* framebuf, u32 stride_pixels, int x, int y, u32 color) {
    if (x < 0 || x >= FB_WIDTH || y < 0 || y >= FB_HEIGHT) {
        return;
    }
    framebuf[y * stride_pixels + x] = color;
}

static void draw_line(u32* framebuf, u32 stride_pixels, int start_x, int start_y, int end_x, int end_y, u32 color) {
    int dx = abs_int(end_x - start_x);
    int sx = start_x < end_x ? 1 : -1;
    int dy = -abs_int(end_y - start_y);
    int sy = start_y < end_y ? 1 : -1;
    int error = dx + dy;

    while (true) {
        draw_pixel(framebuf, stride_pixels, start_x, start_y, color);
        if (start_x == end_x && start_y == end_y) {
            break;
        }

        int doubled_error = error * 2;
        if (doubled_error >= dy) {
            error += dy;
            start_x += sx;
        }
        if (doubled_error <= dx) {
            error += dx;
            start_y += sy;
        }
    }
}

static u32 block_sprite_color(int frame) {
    int safe_frame = frame < 0 ? -frame : frame;
    int column = safe_frame % 6;
    int row = (safe_frame / 6) % 5;
    u32 palette[6] = {
        RGBA8_MAXALPHA(231, 64, 60),
        RGBA8_MAXALPHA(245, 138, 42),
        RGBA8_MAXALPHA(248, 209, 72),
        RGBA8_MAXALPHA(70, 191, 106),
        RGBA8_MAXALPHA(62, 140, 223),
        RGBA8_MAXALPHA(156, 91, 216)
    };

    return color_add(palette[column], row * 12);
}

static void draw_sprite(u32* framebuf, u32 stride_pixels, int x, int y, int width, int height, u32 tint, int sprite_id, int frame) {
    if (width <= 0 || height <= 0) {
        return;
    }

    int left = clamp_int(x, 0, FB_WIDTH);
    int top = clamp_int(y, 0, FB_HEIGHT);
    int right = clamp_int(x + width, 0, FB_WIDTH);
    int bottom = clamp_int(y + height, 0, FB_HEIGHT);
    int is_block_sheet = sprite_id == 394425416 || sprite_id == -1106270640;

    if (is_block_sheet) {
        u32 base = block_sprite_color(frame);
        u32 highlight = color_add(base, 36);
        u32 shadow = color_add(base, -42);
        u32 border = RGBA8_MAXALPHA(18, 22, 30);

        for (int py = top; py < bottom; ++py) {
            u32* row = framebuf + py * stride_pixels;
            for (int px = left; px < right; ++px) {
                int local_x = px - x;
                int local_y = py - y;
                int border_pixel = local_x <= 1 || local_y <= 1 || local_x >= width - 2 || local_y >= height - 2;
                int highlight_pixel = local_x < width / 3 && local_y < height / 3;
                int shadow_pixel = local_x > (width * 2) / 3 || local_y > (height * 2) / 3;
                u32 color = border_pixel ? border : shadow_pixel ? shadow : highlight_pixel ? highlight : base;
                row[px] = color_tint(color, tint);
            }
        }
        return;
    }

    int radius = 30;
    int radius_sq = radius * radius;
    int inner_radius_sq = 23 * 23;
    int safe_frame = frame < 0 ? -frame : frame;
    int palette_index = safe_frame % 6;
    u32 palette[6] = {
        RGBA8_MAXALPHA(231, 64, 60),
        RGBA8_MAXALPHA(245, 138, 42),
        RGBA8_MAXALPHA(248, 209, 72),
        RGBA8_MAXALPHA(70, 191, 106),
        RGBA8_MAXALPHA(62, 140, 223),
        RGBA8_MAXALPHA(156, 91, 216)
    };

    for (int py = top; py < bottom; ++py) {
        int local_y = ((py - y) * 64) / height - 32;
        u32* row = framebuf + py * stride_pixels;

        for (int px = left; px < right; ++px) {
            int local_x = ((px - x) * 64) / width - 32;
            int distance_sq = local_x * local_x + local_y * local_y;
            if (distance_sq > radius_sq) {
                continue;
            }

            u32 color;
            if (sprite_id == 1145756846) {
                if (distance_sq > inner_radius_sq) {
                    color = RGBA8_MAXALPHA(24, 28, 34);
                } else if (abs_int(local_y) <= 5) {
                    color = RGBA8_MAXALPHA(24, 28, 34);
                } else if (local_y < 0) {
                    color = RGBA8_MAXALPHA(226, 42, 52);
                } else {
                    color = RGBA8_MAXALPHA(240, 242, 236);
                }

                int button_distance_sq = local_x * local_x + local_y * local_y;
                if (button_distance_sq <= 7 * 7) {
                    color = button_distance_sq <= 4 * 4
                        ? RGBA8_MAXALPHA(238, 240, 234)
                        : RGBA8_MAXALPHA(24, 28, 34);
                }
            } else {
                int stripe = ((local_x + local_y + safe_frame * 7) / 8) & 1;
                if (distance_sq > inner_radius_sq) {
                    color = RGBA8_MAXALPHA(18, 22, 30);
                } else if (stripe == 0) {
                    color = palette[palette_index];
                } else {
                    color = palette[(palette_index + 3) % 6];
                }
            }

            row[px] = color_tint(color, tint);
        }
    }
}

static void draw_background(u32* framebuf, u32 stride_pixels, u32 background, u32 accent, int pulse_seed) {
    for (int y = 0; y < FB_HEIGHT; ++y) {
        int amount = (y * 120) / FB_HEIGHT;
        int pulse = ((pulse_seed + y) >> 4) & 15;
        u32 row_color = color_add(color_mix(background, accent, amount, 255), pulse);
        u32* row = framebuf + y * stride_pixels;

        for (int x = 0; x < FB_WIDTH; ++x) {
            row[x] = row_color;
        }
    }
}

#define TEXT_GLYPH_WIDTH 5
#define TEXT_GLYPH_HEIGHT 7
#define TEXT_GLYPH_ADVANCE 6
#define TEXT_SPACE_WIDTH 4
#define TEXT_LINE_HEIGHT 8
#define TEXT_GLYPH(a, b, c, d, e, f, g) \
    (((u64)(a) << 30) | ((u64)(b) << 25) | ((u64)(c) << 20) | ((u64)(d) << 15) | \
     ((u64)(e) << 10) | ((u64)(f) << 5) | (u64)(g))

static char ascii_upper(char value) {
    if (value >= 'a' && value <= 'z') {
        return (char)(value - 32);
    }
    return value;
}

static u64 text_glyph_bits(char value) {
    switch (ascii_upper(value)) {
        case '0': return TEXT_GLYPH(0x0e, 0x11, 0x13, 0x15, 0x19, 0x11, 0x0e);
        case '1': return TEXT_GLYPH(0x04, 0x0c, 0x04, 0x04, 0x04, 0x04, 0x0e);
        case '2': return TEXT_GLYPH(0x0e, 0x11, 0x01, 0x02, 0x04, 0x08, 0x1f);
        case '3': return TEXT_GLYPH(0x1e, 0x01, 0x01, 0x0e, 0x01, 0x01, 0x1e);
        case '4': return TEXT_GLYPH(0x02, 0x06, 0x0a, 0x12, 0x1f, 0x02, 0x02);
        case '5': return TEXT_GLYPH(0x1f, 0x10, 0x10, 0x1e, 0x01, 0x01, 0x1e);
        case '6': return TEXT_GLYPH(0x0e, 0x10, 0x10, 0x1e, 0x11, 0x11, 0x0e);
        case '7': return TEXT_GLYPH(0x1f, 0x01, 0x02, 0x04, 0x08, 0x08, 0x08);
        case '8': return TEXT_GLYPH(0x0e, 0x11, 0x11, 0x0e, 0x11, 0x11, 0x0e);
        case '9': return TEXT_GLYPH(0x0e, 0x11, 0x11, 0x0f, 0x01, 0x01, 0x0e);
        case 'A': return TEXT_GLYPH(0x0e, 0x11, 0x11, 0x1f, 0x11, 0x11, 0x11);
        case 'B': return TEXT_GLYPH(0x1e, 0x11, 0x11, 0x1e, 0x11, 0x11, 0x1e);
        case 'C': return TEXT_GLYPH(0x0e, 0x11, 0x10, 0x10, 0x10, 0x11, 0x0e);
        case 'D': return TEXT_GLYPH(0x1e, 0x11, 0x11, 0x11, 0x11, 0x11, 0x1e);
        case 'E': return TEXT_GLYPH(0x1f, 0x10, 0x10, 0x1e, 0x10, 0x10, 0x1f);
        case 'F': return TEXT_GLYPH(0x1f, 0x10, 0x10, 0x1e, 0x10, 0x10, 0x10);
        case 'G': return TEXT_GLYPH(0x0e, 0x11, 0x10, 0x17, 0x11, 0x11, 0x0f);
        case 'H': return TEXT_GLYPH(0x11, 0x11, 0x11, 0x1f, 0x11, 0x11, 0x11);
        case 'I': return TEXT_GLYPH(0x0e, 0x04, 0x04, 0x04, 0x04, 0x04, 0x0e);
        case 'J': return TEXT_GLYPH(0x07, 0x02, 0x02, 0x02, 0x12, 0x12, 0x0c);
        case 'K': return TEXT_GLYPH(0x11, 0x12, 0x14, 0x18, 0x14, 0x12, 0x11);
        case 'L': return TEXT_GLYPH(0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x1f);
        case 'M': return TEXT_GLYPH(0x11, 0x1b, 0x15, 0x15, 0x11, 0x11, 0x11);
        case 'N': return TEXT_GLYPH(0x11, 0x19, 0x15, 0x13, 0x11, 0x11, 0x11);
        case 'O': return TEXT_GLYPH(0x0e, 0x11, 0x11, 0x11, 0x11, 0x11, 0x0e);
        case 'P': return TEXT_GLYPH(0x1e, 0x11, 0x11, 0x1e, 0x10, 0x10, 0x10);
        case 'Q': return TEXT_GLYPH(0x0e, 0x11, 0x11, 0x11, 0x15, 0x12, 0x0d);
        case 'R': return TEXT_GLYPH(0x1e, 0x11, 0x11, 0x1e, 0x14, 0x12, 0x11);
        case 'S': return TEXT_GLYPH(0x0f, 0x10, 0x10, 0x0e, 0x01, 0x01, 0x1e);
        case 'T': return TEXT_GLYPH(0x1f, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04);
        case 'U': return TEXT_GLYPH(0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x0e);
        case 'V': return TEXT_GLYPH(0x11, 0x11, 0x11, 0x11, 0x11, 0x0a, 0x04);
        case 'W': return TEXT_GLYPH(0x11, 0x11, 0x11, 0x15, 0x15, 0x15, 0x0a);
        case 'X': return TEXT_GLYPH(0x11, 0x11, 0x0a, 0x04, 0x0a, 0x11, 0x11);
        case 'Y': return TEXT_GLYPH(0x11, 0x11, 0x0a, 0x04, 0x04, 0x04, 0x04);
        case 'Z': return TEXT_GLYPH(0x1f, 0x01, 0x02, 0x04, 0x08, 0x10, 0x1f);
        case ':': return TEXT_GLYPH(0x00, 0x04, 0x04, 0x00, 0x04, 0x04, 0x00);
        case '-': return TEXT_GLYPH(0x00, 0x00, 0x00, 0x1f, 0x00, 0x00, 0x00);
        case '/': return TEXT_GLYPH(0x01, 0x01, 0x02, 0x04, 0x08, 0x10, 0x10);
        case '.': return TEXT_GLYPH(0x00, 0x00, 0x00, 0x00, 0x00, 0x0c, 0x0c);
        case ',': return TEXT_GLYPH(0x00, 0x00, 0x00, 0x00, 0x0c, 0x04, 0x08);
        case '+': return TEXT_GLYPH(0x00, 0x04, 0x04, 0x1f, 0x04, 0x04, 0x00);
        case '!': return TEXT_GLYPH(0x04, 0x04, 0x04, 0x04, 0x04, 0x00, 0x04);
        case '_': return TEXT_GLYPH(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1f);
        case '=': return TEXT_GLYPH(0x00, 0x00, 0x1f, 0x00, 0x1f, 0x00, 0x00);
        case '\'': return TEXT_GLYPH(0x04, 0x04, 0x08, 0x00, 0x00, 0x00, 0x00);
        default: return TEXT_GLYPH(0x0e, 0x11, 0x01, 0x02, 0x04, 0x00, 0x04);
    }
}

static void draw_text(u32* framebuf, u32 stride_pixels, const char* text, int x, int y, u32 color, int scale) {
    if (text == NULL) {
        return;
    }

    int safe_scale = clamp_int(scale, 1, 12);
    int cursor_x = x;
    int cursor_y = y;

    for (const char* cursor = text; *cursor != '\0'; ++cursor) {
        char value = *cursor;
        if (value == '\n') {
            cursor_x = x;
            cursor_y += TEXT_LINE_HEIGHT * safe_scale;
            continue;
        }
        if (value == ' ') {
            cursor_x += TEXT_SPACE_WIDTH * safe_scale;
            continue;
        }

        u64 bits = text_glyph_bits(value);
        for (int row = 0; row < TEXT_GLYPH_HEIGHT; ++row) {
            int row_bits = (int)((bits >> ((TEXT_GLYPH_HEIGHT - 1 - row) * TEXT_GLYPH_WIDTH)) & 0x1f);
            for (int column = 0; column < TEXT_GLYPH_WIDTH; ++column) {
                if ((row_bits & (1 << (TEXT_GLYPH_WIDTH - 1 - column))) != 0) {
                    draw_rect(
                        framebuf,
                        stride_pixels,
                        cursor_x + column * safe_scale,
                        cursor_y + row * safe_scale,
                        safe_scale,
                        safe_scale,
                        color
                    );
                }
            }
        }

        cursor_x += TEXT_GLYPH_ADVANCE * safe_scale;
    }
}

static void execute_render_command(
    u32* framebuf,
    u32 stride_pixels,
    const kengine_switch_kotlin_KInt* commands,
    int command_index
) {
    int offset = command_index * KENGINE_RENDER_FIELD_COUNT;
    int type = commands[offset + KENGINE_RENDER_FIELD_TYPE];
    int x = commands[offset + KENGINE_RENDER_FIELD_X];
    int y = commands[offset + KENGINE_RENDER_FIELD_Y];
    int width = commands[offset + KENGINE_RENDER_FIELD_WIDTH];
    int height = commands[offset + KENGINE_RENDER_FIELD_HEIGHT];
    u32 color = (u32)commands[offset + KENGINE_RENDER_FIELD_COLOR];
    u32 color2 = (u32)commands[offset + KENGINE_RENDER_FIELD_COLOR2];
    int param = commands[offset + KENGINE_RENDER_FIELD_PARAM];

    switch (type) {
        case KENGINE_RENDER_CLEAR:
            draw_rect(framebuf, stride_pixels, 0, 0, FB_WIDTH, FB_HEIGHT, color);
            break;
        case KENGINE_RENDER_FILL_RECT:
            draw_rect(framebuf, stride_pixels, x, y, width, height, color);
            break;
        case KENGINE_RENDER_DRAW_LINE:
            draw_line(framebuf, stride_pixels, x, y, width, height, color);
            break;
        case KENGINE_RENDER_DRAW_SPRITE:
            draw_sprite(framebuf, stride_pixels, x, y, width, height, color, (int)color2, param);
            break;
        case KENGINE_RENDER_DRAW_TEXT: {
            const char* text = kotlin_command_text(command_index);
            draw_text(framebuf, stride_pixels, text, x, y, color, width);
            kotlin_dispose_string(text);
            break;
        }
        case KENGINE_RENDER_VERTICAL_GRADIENT:
            draw_background(framebuf, stride_pixels, color, color2, param);
            break;
        default:
            break;
    }
}

static void draw_kengine_frame(Framebuffer* framebuffer) {
    u32 stride;
    u32* framebuf = (u32*)framebufferBegin(framebuffer, &stride);
    if (framebuf == NULL) {
        return;
    }

    u32 stride_pixels = stride / sizeof(u32);
    int command_count = clamp_int(kotlin_copy_commands(g_render_commands, KENGINE_RENDER_MAX_COMMANDS), 0, KENGINE_RENDER_MAX_COMMANDS);
    for (int index = 0; index < command_count; ++index) {
        execute_render_command(framebuf, stride_pixels, g_render_commands, index);
    }

    framebufferEnd(framebuffer);
}

#ifdef KENGINE_SWITCH_HEXTRIS_MUSIC
extern const u8 _binary_music_pcm_start[];
extern const u8 _binary_music_pcm_end[];

#define KENGINE_AUDIO_BUFFER_COUNT 4
#define KENGINE_AUDIO_SAMPLE_RATE 48000
#define KENGINE_AUDIO_CHANNEL_COUNT 2
#define KENGINE_AUDIO_BYTES_PER_SAMPLE 2
#define KENGINE_AUDIO_SAMPLES_PER_BUFFER 8192
#define KENGINE_AUDIO_DATA_SIZE (KENGINE_AUDIO_SAMPLES_PER_BUFFER * KENGINE_AUDIO_CHANNEL_COUNT * KENGINE_AUDIO_BYTES_PER_SAMPLE)
#define KENGINE_AUDIO_BUFFER_SIZE ((KENGINE_AUDIO_DATA_SIZE + 0xfff) & ~0xfff)

typedef struct {
    AudioOutBuffer buffers[KENGINE_AUDIO_BUFFER_COUNT];
    u8* buffer_data;
    size_t music_offset;
    bool audout_initialized;
    bool audout_started;
    bool playing;
    int current_asset_id;
} KengineSwitchAudioState;

static KengineSwitchAudioState g_audio_state;

static size_t kengine_switch_music_size(void) {
    return (size_t)(_binary_music_pcm_end - _binary_music_pcm_start);
}

static void kengine_switch_audio_fill(void* destination, size_t destination_size) {
    size_t music_size = kengine_switch_music_size();
    if (destination == NULL || destination_size == 0 || music_size == 0) {
        return;
    }

    u8* output = (u8*)destination;
    size_t written = 0;
    while (written < destination_size) {
        size_t remaining_music = music_size - g_audio_state.music_offset;
        size_t remaining_output = destination_size - written;
        size_t chunk_size = remaining_music < remaining_output ? remaining_music : remaining_output;

        memcpy(output + written, _binary_music_pcm_start + g_audio_state.music_offset, chunk_size);
        written += chunk_size;
        g_audio_state.music_offset += chunk_size;
        if (g_audio_state.music_offset >= music_size) {
            g_audio_state.music_offset = 0;
        }
    }

    armDCacheFlush(destination, destination_size);
}

static bool kengine_switch_audio_queue_buffer(AudioOutBuffer* buffer) {
    kengine_switch_audio_fill(buffer->buffer, buffer->data_size);
    return R_SUCCEEDED(audoutAppendAudioOutBuffer(buffer));
}

static void kengine_switch_audio_stop(void) {
    if (g_audio_state.audout_started) {
        audoutStopAudioOut();
    }
    if (g_audio_state.audout_initialized) {
        audoutExit();
    }
    if (g_audio_state.buffer_data != NULL) {
        free(g_audio_state.buffer_data);
    }

    memset(&g_audio_state, 0, sizeof(g_audio_state));
}

static void kengine_switch_audio_loop_music(int asset_id, int volume) {
    if (asset_id == 0) {
        return;
    }
    if (g_audio_state.playing && g_audio_state.current_asset_id == asset_id) {
        audoutSetAudioOutVolume((float)clamp_int(volume, 0, 255) / 255.0f);
        return;
    }
    if (g_audio_state.audout_initialized) {
        kengine_switch_audio_stop();
    }

    memset(&g_audio_state, 0, sizeof(g_audio_state));
    g_audio_state.current_asset_id = asset_id;

    size_t music_size = kengine_switch_music_size();
    if (music_size == 0) {
        return;
    }

    Result result = audoutInitialize();
    if (R_FAILED(result)) {
        return;
    }
    g_audio_state.audout_initialized = true;

    if (audoutGetSampleRate() != KENGINE_AUDIO_SAMPLE_RATE ||
        audoutGetChannelCount() != KENGINE_AUDIO_CHANNEL_COUNT ||
        audoutGetPcmFormat() != PcmFormat_Int16) {
        kengine_switch_audio_stop();
        return;
    }

    g_audio_state.buffer_data = (u8*)memalign(0x1000, KENGINE_AUDIO_BUFFER_SIZE * KENGINE_AUDIO_BUFFER_COUNT);
    if (g_audio_state.buffer_data == NULL) {
        kengine_switch_audio_stop();
        return;
    }
    memset(g_audio_state.buffer_data, 0, KENGINE_AUDIO_BUFFER_SIZE * KENGINE_AUDIO_BUFFER_COUNT);

    result = audoutStartAudioOut();
    if (R_FAILED(result)) {
        kengine_switch_audio_stop();
        return;
    }
    g_audio_state.audout_started = true;
    g_audio_state.playing = true;
    audoutSetAudioOutVolume((float)clamp_int(volume, 0, 255) / 255.0f);

    for (int index = 0; index < KENGINE_AUDIO_BUFFER_COUNT; ++index) {
        AudioOutBuffer* buffer = &g_audio_state.buffers[index];
        buffer->next = NULL;
        buffer->buffer = g_audio_state.buffer_data + (KENGINE_AUDIO_BUFFER_SIZE * index);
        buffer->buffer_size = KENGINE_AUDIO_BUFFER_SIZE;
        buffer->data_size = KENGINE_AUDIO_DATA_SIZE;
        buffer->data_offset = 0;

        if (!kengine_switch_audio_queue_buffer(buffer)) {
            kengine_switch_audio_stop();
            return;
        }
    }
}

static void kengine_switch_audio_update(void) {
    if (!g_audio_state.playing) {
        return;
    }

    for (int attempt = 0; attempt < KENGINE_AUDIO_BUFFER_COUNT; ++attempt) {
        AudioOutBuffer* released_buffer = NULL;
        u32 released_count = 0;
        Result result = audoutGetReleasedAudioOutBuffer(&released_buffer, &released_count);
        if (R_FAILED(result) || released_buffer == NULL || released_count == 0) {
            return;
        }

        if (!kengine_switch_audio_queue_buffer(released_buffer)) {
            g_audio_state.playing = false;
            return;
        }
    }
}
#else
static void kengine_switch_audio_loop_music(int asset_id, int volume) {
    (void)asset_id;
    (void)volume;
}

static void kengine_switch_audio_update(void) {
}

static void kengine_switch_audio_stop(void) {
}
#endif

static void execute_audio_command(const kengine_switch_kotlin_KInt* commands, int command_index) {
    int offset = command_index * KENGINE_AUDIO_FIELD_COUNT;
    int type = commands[offset + KENGINE_AUDIO_FIELD_TYPE];
    int asset_id = commands[offset + KENGINE_AUDIO_FIELD_ASSET_ID];
    int volume = commands[offset + KENGINE_AUDIO_FIELD_VOLUME];

    switch (type) {
        case KENGINE_AUDIO_LOOP_MUSIC:
            kengine_switch_audio_loop_music(asset_id, volume);
            break;
        case KENGINE_AUDIO_STOP_MUSIC:
            kengine_switch_audio_stop();
            break;
        default:
            break;
    }
}

static void execute_audio_commands(void) {
    int command_count = clamp_int(kotlin_copy_audio_commands(g_audio_commands, KENGINE_AUDIO_MAX_COMMANDS), 0, KENGINE_AUDIO_MAX_COMMANDS);
    for (int index = 0; index < command_count; ++index) {
        execute_audio_command(g_audio_commands, index);
    }
}

static int run_kotlin_framebuffer_demo(void) {
    NWindow* window = nwindowGetDefault();

    Framebuffer framebuffer;
    Result result = framebufferCreate(&framebuffer, window, FB_WIDTH, FB_HEIGHT, PIXEL_FORMAT_RGBA_8888, 2);
    if (R_FAILED(result)) {
        return 1;
    }

    result = framebufferMakeLinear(&framebuffer);
    if (R_FAILED(result)) {
        framebufferClose(&framebuffer);
        return 1;
    }

    PadState pad;
    padConfigureInput(1, HidNpadStyleSet_NpadStandard);
    padInitializeDefault(&pad);

    int diagnostic_checksum = kotlin_add_probe() ^ kotlin_message_code_probe() ^ kotlin_startup_probe();
    kotlin_runtime_start();

    int frame_count = 0;
    while (appletMainLoop()) {
        padUpdate(&pad);

        u64 buttons_down = padGetButtonsDown(&pad);
        u64 buttons = padGetButtons(&pad);
        if ((buttons_down & HidNpadButton_Plus) && (buttons & HidNpadButton_Minus)) {
            break;
        }

        int input_mask = input_mask_from_buttons(buttons);
        int update_checksum = kotlin_runtime_update(frame_count, input_mask);
        int audio_checksum = kotlin_runtime_audio(frame_count);
        execute_audio_commands();
        int draw_checksum = kotlin_runtime_draw(frame_count);

        if ((frame_count % 120) == 0) {
            diagnostic_checksum ^= kotlin_allocation_probe(frame_count + 1, 16) ^ update_checksum ^ audio_checksum ^ draw_checksum;
            kotlin_runtime_snapshot();
        }

        draw_kengine_frame(&framebuffer);
        kengine_switch_audio_update();
        frame_count += 1;
    }

    (void)diagnostic_checksum;
    kengine_switch_audio_stop();
    kotlin_runtime_cleanup();
    framebufferClose(&framebuffer);
    return 0;
}
#endif

int main(int argc, char* argv[]) {
    (void)argc;
    (void)argv;

#ifdef KENGINE_SWITCH_C_ONLY
    return run_console_smoke_test();
#else
    return run_kotlin_framebuffer_demo();
#endif
}
