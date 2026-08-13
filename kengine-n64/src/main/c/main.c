#include <stdio.h>
#include <stdbool.h>
#include <string.h>
#include <libdragon.h>
#ifndef KENGINE_N64_USE_RDPQ_RENDER
#define KENGINE_N64_USE_RDPQ_RENDER 0
#endif
#if KENGINE_N64_USE_RDPQ_RENDER
#include <rdpq.h>
#include <rdpq_attach.h>
#include <rdpq_mode.h>
#include <rdpq_rect.h>
#include <rdpq_tri.h>
#include <rdpq_tex.h>
#endif

#ifndef KENGINE_N64_C_ONLY
#include "kengine_n64_kotlin_api.h"
#endif
#ifdef KENGINE_N64_SPRITE_ASSETS
#include "kengine_n64_sprite_assets.h"
#endif
#ifdef KENGINE_N64_SOUND_ASSETS
#include "kengine_n64_sound_assets.h"
#endif
#ifdef KENGINE_N64_WORLD_MESH
#include "kengine_n64_world_mesh.h"
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
#define KENGINE_INPUT_C_UP (1 << 7)
#define KENGINE_INPUT_C_DOWN (1 << 8)
#define KENGINE_INPUT_L (1 << 9)
#define KENGINE_INPUT_R (1 << 10)
#define KENGINE_INPUT_Z (1 << 11)
#define KENGINE_INPUT_C_LEFT (1 << 12)
#define KENGINE_INPUT_C_RIGHT (1 << 13)

#define KENGINE_RENDER_CLEAR 1
#define KENGINE_RENDER_FILL_RECT 2
#define KENGINE_RENDER_VERTICAL_GRADIENT 3
#define KENGINE_RENDER_DRAW_LINE 4
#define KENGINE_RENDER_DRAW_SPRITE 5
#define KENGINE_RENDER_DRAW_TEXT 6
#define KENGINE_RENDER_DRAW_TRIANGLE 7
#define KENGINE_RENDER_DRAW_WORLD_3D 8

#define KENGINE_RENDER_FIELD_TYPE 0
#define KENGINE_RENDER_FIELD_X 1
#define KENGINE_RENDER_FIELD_Y 2
#define KENGINE_RENDER_FIELD_WIDTH 3
#define KENGINE_RENDER_FIELD_HEIGHT 4
#define KENGINE_RENDER_FIELD_COLOR 5
#define KENGINE_RENDER_FIELD_COLOR2 6
#define KENGINE_RENDER_FIELD_PARAM 7
#define KENGINE_RENDER_FIELD_COUNT 8
#define KENGINE_RENDER_MAX_COMMANDS 512

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

#define KENGINE_AUDIO_SAMPLE_RATE 22050
#define KENGINE_AUDIO_NUM_CHANNELS 4
#define KENGINE_AUDIO_MUSIC_CHANNEL 0
#define KENGINE_AUDIO_FIRST_SOUND_CHANNEL 1
#define KENGINE_PERF_OVERLAY_X 4
#define KENGINE_PERF_OVERLAY_Y 2
#define KENGINE_PERF_OVERLAY_LINE_HEIGHT 10

/* In-memory PCM waveform for the mixer */
#ifdef KENGINE_N64_SOUND_ASSETS
typedef struct {
    waveform_t wave;
    const int16_t* samples;
    int num_samples;
} kengine_pcm_wave_t;

static void kengine_pcm_read(void* ctx, samplebuffer_t* sbuf, int wpos, int wlen, bool seeking) {
    (void)seeking;
    kengine_pcm_wave_t* pcm = (kengine_pcm_wave_t*)ctx;
    int16_t* dest = (int16_t*)samplebuffer_append(sbuf, wlen);
    for (int i = 0; i < wlen; i++) {
        int idx = wpos + i;
        if (pcm->wave.loop_len > 0 && pcm->num_samples > 0) {
            idx %= pcm->num_samples;
        }
        dest[i] = idx < pcm->num_samples ? pcm->samples[idx] : 0;
    }
}

static kengine_pcm_wave_t g_sound_waves[KENGINE_AUDIO_NUM_CHANNELS];
static int g_current_music_asset_id = 0;
static int g_current_music_volume = -1;

static void kengine_audio_callback(int16_t* buffer, size_t nsamples) {
    mixer_poll(buffer, nsamples);
}

static void kengine_audio_init(void) {
    audio_init(KENGINE_AUDIO_SAMPLE_RATE, 4);
    mixer_init(KENGINE_AUDIO_NUM_CHANNELS);
    audio_set_buffer_callback(kengine_audio_callback);
    audio_write_silence();
}

static bool kengine_play_audio_asset(int asset_id, int channel, int volume, bool loop, const char* name) {
    const KengineN64SoundAsset* asset = kengine_n64_find_sound_asset(asset_id);
    if (!asset) return false;

    const int16_t* samples = (const int16_t*)asset->data_start;
    int byte_count = (int)(asset->data_end - asset->data_start);
    int num_samples = byte_count / 2;

    mixer_ch_stop(channel);

    /* Set up a resident PCM waveform for the mixer */
    kengine_pcm_wave_t* pcm = &g_sound_waves[channel];
    memset(pcm, 0, sizeof(*pcm));
    pcm->samples = samples;
    pcm->num_samples = num_samples;

    pcm->wave.name = name;
    pcm->wave.bits = 16;
    pcm->wave.channels = 1;
    pcm->wave.frequency = KENGINE_AUDIO_SAMPLE_RATE;
    pcm->wave.len = num_samples;
    pcm->wave.loop_len = loop ? num_samples : 0;
    pcm->wave.read = kengine_pcm_read;
    pcm->wave.ctx = pcm;

    float vol = (float)volume / 255.0f;
    mixer_ch_set_vol(channel, vol, vol);
    mixer_ch_play(channel, &pcm->wave);
    return true;
}

static void kengine_play_sound(int asset_id, int volume) {
    static int next_channel = KENGINE_AUDIO_FIRST_SOUND_CHANNEL;
    int ch = next_channel;
    next_channel++;
    if (next_channel >= KENGINE_AUDIO_NUM_CHANNELS) {
        next_channel = KENGINE_AUDIO_FIRST_SOUND_CHANNEL;
    }

    kengine_play_audio_asset(asset_id, ch, volume, false, "kengine_sfx");
}

static void kengine_loop_music(int asset_id, int volume) {
    if (g_current_music_asset_id == asset_id) {
        if (g_current_music_volume != volume) {
            float vol = (float)volume / 255.0f;
            mixer_ch_set_vol(KENGINE_AUDIO_MUSIC_CHANNEL, vol, vol);
            g_current_music_volume = volume;
        }
        return;
    }

    if (kengine_play_audio_asset(asset_id, KENGINE_AUDIO_MUSIC_CHANNEL, volume, true, "kengine_music")) {
        g_current_music_asset_id = asset_id;
        g_current_music_volume = volume;
    }
}

static void kengine_stop_music(int asset_id) {
    if (g_current_music_asset_id == 0) {
        return;
    }
    if (asset_id != 0 && asset_id != g_current_music_asset_id) {
        return;
    }

    mixer_ch_stop(KENGINE_AUDIO_MUSIC_CHANNEL);
    g_current_music_asset_id = 0;
    g_current_music_volume = -1;
}

static void execute_audio_commands(int* commands, int command_count) {
    for (int i = 0; i < command_count; i++) {
        int base = i * KENGINE_AUDIO_FIELD_COUNT;
        int type = commands[base + KENGINE_AUDIO_FIELD_TYPE];
        int asset_id = commands[base + KENGINE_AUDIO_FIELD_ASSET_ID];
        int volume = commands[base + KENGINE_AUDIO_FIELD_VOLUME];

        switch (type) {
            case KENGINE_AUDIO_LOOP_MUSIC:
                kengine_loop_music(asset_id, volume);
                break;
            case KENGINE_AUDIO_PLAY_SOUND:
                kengine_play_sound(asset_id, volume);
                break;
            case KENGINE_AUDIO_STOP_MUSIC:
                kengine_stop_music(asset_id);
                break;
            default:
                break;
        }
    }
}
#endif

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
    uint8_t r = rgba & 0xFF;
    uint8_t g = (rgba >> 8) & 0xFF;
    uint8_t b = (rgba >> 16) & 0xFF;
    uint8_t a = (rgba >> 24) & 0xFF;
    return graphics_make_color(r, g, b, a);
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

#if KENGINE_N64_USE_RDPQ_RENDER
static color_t kengine_rgba_to_rdpq_color(int rgba) {
    uint8_t r = rgba & 0xFF;
    uint8_t g = (rgba >> 8) & 0xFF;
    uint8_t b = (rgba >> 16) & 0xFF;
    uint8_t a = (rgba >> 24) & 0xFF;
    return RGBA32(r, g, b, a);
}

typedef enum {
    KENGINE_RDPQ_MODE_NONE = 0,
    KENGINE_RDPQ_MODE_FILL = 1,
    KENGINE_RDPQ_MODE_FLAT_TRIANGLE = 2,
    KENGINE_RDPQ_MODE_ZBUF_TRIANGLE = 3,
} KengineRdpqMode;

static int g_rdpq_attached = 0;
static KengineRdpqMode g_rdpq_mode = KENGINE_RDPQ_MODE_NONE;
static surface_t g_zbuffer;
static int g_zbuffer_allocated = 0;

static void kengine_ensure_zbuffer(void) {
    if (!g_zbuffer_allocated) {
        g_zbuffer = surface_alloc(FMT_RGBA16, FB_WIDTH, FB_HEIGHT);
        g_zbuffer_allocated = 1;
    }
}

static void rdpq_begin(surface_t* disp) {
    if (g_rdpq_attached) {
        return;
    }
    rdpq_attach(disp, NULL);
    g_rdpq_attached = 1;
    g_rdpq_mode = KENGINE_RDPQ_MODE_NONE;
}

static void rdpq_begin_zbuf(surface_t* disp) {
    if (g_rdpq_attached) {
        return;
    }
    kengine_ensure_zbuffer();
    rdpq_attach(disp, &g_zbuffer);
    rdpq_clear_z(0xFFFC);
    g_rdpq_attached = 1;
    g_rdpq_mode = KENGINE_RDPQ_MODE_NONE;
}

static void rdpq_flush(void) {
    if (!g_rdpq_attached) {
        return;
    }
    rdpq_detach_wait();
    g_rdpq_attached = 0;
    g_rdpq_mode = KENGINE_RDPQ_MODE_NONE;
}

static void rdpq_prepare_fill(color_t color) {
    if (g_rdpq_mode != KENGINE_RDPQ_MODE_FILL) {
        rdpq_set_mode_fill(color);
        g_rdpq_mode = KENGINE_RDPQ_MODE_FILL;
    } else {
        rdpq_set_fill_color(color);
    }
}

static void rdpq_prepare_flat_triangle(void) {
    if (g_rdpq_mode == KENGINE_RDPQ_MODE_FLAT_TRIANGLE) {
        return;
    }
    rdpq_set_mode_standard();
    rdpq_mode_combiner(RDPQ_COMBINER_FLAT);
    g_rdpq_mode = KENGINE_RDPQ_MODE_FLAT_TRIANGLE;
}

static void rdpq_prepare_zbuf_triangle(void) {
    if (g_rdpq_mode == KENGINE_RDPQ_MODE_ZBUF_TRIANGLE) {
        return;
    }
    rdpq_set_mode_standard();
    rdpq_mode_combiner(RDPQ_COMBINER_FLAT);
    rdpq_mode_zbuf(true, true);
    g_rdpq_mode = KENGINE_RDPQ_MODE_ZBUF_TRIANGLE;
}

static void draw_rect_rdpq(surface_t* disp, int x, int y, int width, int height, int color) {
    int x0 = x < 0 ? 0 : x;
    int y0 = y < 0 ? 0 : y;
    int x1 = x + width;
    int y1 = y + height;
    if (x1 > FB_WIDTH) x1 = FB_WIDTH;
    if (y1 > FB_HEIGHT) y1 = FB_HEIGHT;

    if (x0 >= x1 || y0 >= y1) return;

    rdpq_begin(disp);
    rdpq_prepare_fill(kengine_rgba_to_rdpq_color(color));
    rdpq_fill_rectangle(x0, y0, x1, y1);
}

static void clear_rdpq(surface_t* disp, int color) {
    rdpq_begin(disp);
    rdpq_prepare_fill(kengine_rgba_to_rdpq_color(color));
    rdpq_fill_rectangle(0, 0, FB_WIDTH, FB_HEIGHT);
}
#endif

static int line_clip_outcode(int x, int y) {
    int code = 0;
    if (x < 0) code |= 1;
    else if (x >= FB_WIDTH) code |= 2;
    if (y < 0) code |= 4;
    else if (y >= FB_HEIGHT) code |= 8;
    return code;
}

static int clip_line_to_screen(int* x0, int* y0, int* x1, int* y1) {
    int out0 = line_clip_outcode(*x0, *y0);
    int out1 = line_clip_outcode(*x1, *y1);

    while (1) {
        if ((out0 | out1) == 0) {
            return 1;
        }
        if ((out0 & out1) != 0) {
            return 0;
        }

        int out = out0 != 0 ? out0 : out1;
        int x = 0;
        int y = 0;

        if ((out & 8) != 0) {
            if (*y1 == *y0) return 0;
            y = FB_HEIGHT - 1;
            x = *x0 + (int)(((long long)(*x1 - *x0) * (y - *y0)) / (*y1 - *y0));
        } else if ((out & 4) != 0) {
            if (*y1 == *y0) return 0;
            y = 0;
            x = *x0 + (int)(((long long)(*x1 - *x0) * (y - *y0)) / (*y1 - *y0));
        } else if ((out & 2) != 0) {
            if (*x1 == *x0) return 0;
            x = FB_WIDTH - 1;
            y = *y0 + (int)(((long long)(*y1 - *y0) * (x - *x0)) / (*x1 - *x0));
        } else if ((out & 1) != 0) {
            if (*x1 == *x0) return 0;
            x = 0;
            y = *y0 + (int)(((long long)(*y1 - *y0) * (x - *x0)) / (*x1 - *x0));
        }

        if (out == out0) {
            *x0 = x;
            *y0 = y;
            out0 = line_clip_outcode(*x0, *y0);
        } else {
            *x1 = x;
            *y1 = y;
            out1 = line_clip_outcode(*x1, *y1);
        }
    }
}

static void draw_line(surface_t* disp, int x0, int y0, int x1, int y1, int color) {
    if (!clip_line_to_screen(&x0, &y0, &x1, &y1)) {
        return;
    }

    uint32_t c = kengine_rgba_to_color(color);
    graphics_draw_line(disp, x0, y0, x1, y1, c);
}

static void swap_int(int* a, int* b) {
    int value = *a;
    *a = *b;
    *b = value;
}

static int clamp_int_to_range(int value, int minimum, int maximum) {
    if (value < minimum) return minimum;
    if (value > maximum) return maximum;
    return value;
}

static int interpolate_triangle_x(int x0, int y0, int x1, int y1, int y) {
    if (y0 == y1) return x0;
    return x0 + (int)((((long long)x1 - (long long)x0) * ((long long)y - (long long)y0)) / ((long long)y1 - (long long)y0));
}

static void draw_triangle_span(surface_t* disp, int y, int x0, int x1, uint32_t color) {
    if (y < 0 || y >= FB_HEIGHT) return;
    if (x0 > x1) swap_int(&x0, &x1);
    if (x1 < 0 || x0 >= FB_WIDTH) return;

    x0 = clamp_int_to_range(x0, 0, FB_WIDTH - 1);
    x1 = clamp_int_to_range(x1, 0, FB_WIDTH - 1);
    graphics_draw_box(disp, x0, y, x1 - x0 + 1, 1, color);
}

static void draw_triangle(surface_t* disp, int x0, int y0, int x1, int y1, int x2, int y2, int color) {
    if ((y0 < 0 && y1 < 0 && y2 < 0) || (y0 >= FB_HEIGHT && y1 >= FB_HEIGHT && y2 >= FB_HEIGHT)) return;
    if ((x0 < 0 && x1 < 0 && x2 < 0) || (x0 >= FB_WIDTH && x1 >= FB_WIDTH && x2 >= FB_WIDTH)) return;

    if (y0 > y1) {
        swap_int(&x0, &x1);
        swap_int(&y0, &y1);
    }
    if (y1 > y2) {
        swap_int(&x1, &x2);
        swap_int(&y1, &y2);
    }
    if (y0 > y1) {
        swap_int(&x0, &x1);
        swap_int(&y0, &y1);
    }

    uint32_t native_color = kengine_rgba_to_color(color);
    if (y0 == y2) {
        int left = x0;
        int right = x0;
        if (x1 < left) left = x1;
        if (x2 < left) left = x2;
        if (x1 > right) right = x1;
        if (x2 > right) right = x2;
        draw_triangle_span(disp, y0, left, right, native_color);
        return;
    }

    int y_start = clamp_int_to_range(y0, 0, FB_HEIGHT - 1);
    int y_end = clamp_int_to_range(y2, 0, FB_HEIGHT - 1);
    for (int y = y_start; y <= y_end; ++y) {
        int long_x = interpolate_triangle_x(x0, y0, x2, y2, y);
        int short_x = y < y1
            ? interpolate_triangle_x(x0, y0, x1, y1, y)
            : interpolate_triangle_x(x1, y1, x2, y2, y);
        draw_triangle_span(disp, y, long_x, short_x, native_color);
    }
}

#if KENGINE_N64_USE_RDPQ_RENDER
static void draw_triangle_rdpq(surface_t* disp, int x0, int y0, int x1, int y1, int x2, int y2, int color) {
    if ((y0 < 0 && y1 < 0 && y2 < 0) || (y0 >= FB_HEIGHT && y1 >= FB_HEIGHT && y2 >= FB_HEIGHT)) return;
    if ((x0 < 0 && x1 < 0 && x2 < 0) || (x0 >= FB_WIDTH && x1 >= FB_WIDTH && x2 >= FB_WIDTH)) return;

    rdpq_begin(disp);
    rdpq_prepare_flat_triangle();
    rdpq_set_prim_color(kengine_rgba_to_rdpq_color(color));

    float v1[] = { (float)x0, (float)y0 };
    float v2[] = { (float)x1, (float)y1 };
    float v3[] = { (float)x2, (float)y2 };
    rdpq_triangle(&TRIFMT_FILL, v1, v2, v3);
}
#endif

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

typedef struct {
    int kotlin_us;
    int render_us;
    int native_audio_us;
    int total_us;
} KengineFrameTiming;

static int ticks_to_us(long long ticks) {
    return (int)TIMER_MICROS_LL(ticks);
}

static int us_to_ms_rounded(int us) {
    return (us + 500) / 1000;
}

/* ---------------------------------------------------------------------------
 * Native 3D world renderer — projects vertices in C, renders via rdpq/software
 * --------------------------------------------------------------------------- */
#ifdef KENGINE_N64_WORLD_MESH

#define WORLD3D_TRIG_SCALE 4096
#define WORLD3D_ANGLE_FULL 1024
#define WORLD3D_ANGLE_RIGHT (WORLD3D_ANGLE_FULL / 4)
#define WORLD3D_NEAR_PLANE 20
#define WORLD3D_OFFSCREEN 128
#define WORLD3D_COORD_LIMIT 2048
#define WORLD3D_DEGENERATE_AREA 2
#define WORLD3D_MAX_VERTICES 6500
#define WORLD3D_MAX_VISIBLE 2200
#define WORLD3D_DEPTH_BUCKETS 64

static int w3d_screen_x[WORLD3D_MAX_VERTICES];
static int w3d_screen_y[WORLD3D_MAX_VERTICES];
static int w3d_screen_z[WORLD3D_MAX_VERTICES];
static int w3d_view_x[WORLD3D_MAX_VERTICES];
static int w3d_view_y[WORLD3D_MAX_VERTICES];
static int w3d_view_z[WORLD3D_MAX_VERTICES];
static int w3d_visible[WORLD3D_MAX_VERTICES];
static int w3d_vis_tri[WORLD3D_MAX_VISIBLE];
static int w3d_vis_area[WORLD3D_MAX_VISIBLE];
static int w3d_bucket_head[WORLD3D_DEPTH_BUCKETS];
static int w3d_bucket_tail[WORLD3D_DEPTH_BUCKETS];
static int w3d_bucket_next[WORLD3D_MAX_VISIBLE];

static int w3d_quarter_sin(int v) {
    if (v < 0) v = 0;
    if (v > WORLD3D_ANGLE_RIGHT) v = WORLD3D_ANGLE_RIGHT;
    int num = v * (WORLD3D_ANGLE_RIGHT * 2 - v);
    int den = WORLD3D_ANGLE_RIGHT * WORLD3D_ANGLE_RIGHT;
    int scaled = num * WORLD3D_TRIG_SCALE;
    return scaled >= 0 ? (scaled + den / 2) / den : (scaled - den / 2) / den;
}

static int w3d_sin(int angle) {
    int w = angle & (WORLD3D_ANGLE_FULL - 1);
    if (w < WORLD3D_ANGLE_RIGHT) return w3d_quarter_sin(w);
    if (w < WORLD3D_ANGLE_RIGHT * 2) return w3d_quarter_sin(WORLD3D_ANGLE_RIGHT * 2 - w);
    if (w < WORLD3D_ANGLE_RIGHT * 3) return -w3d_quarter_sin(w - WORLD3D_ANGLE_RIGHT * 2);
    return -w3d_quarter_sin(WORLD3D_ANGLE_FULL - w);
}

static int w3d_cos(int angle) { return w3d_sin(angle + WORLD3D_ANGLE_RIGHT); }

static inline int w3d_abs(int v) { return v < 0 ? -v : v; }
static inline int w3d_clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }

static void w3d_clip_project(int vx0, int vy0, int vz0, int vx1, int vy1, int vz1,
                              int proj_dist, int center_x, int center_y,
                              int* out_sx, int* out_sy, int* out_sz) {
    int dz = vz1 - vz0;
    if (dz == 0) dz = 1;
    int t_num = WORLD3D_NEAR_PLANE + 1 - vz0;
    int cx = vx0 + (vx1 - vx0) * t_num / dz;
    int cy = vy0 + (vy1 - vy0) * t_num / dz;
    int cz = WORLD3D_NEAR_PLANE + 1;
    *out_sx = center_x + (cx * proj_dist + cz / 2) / cz;
    *out_sy = center_y - (cy * proj_dist + cz / 2) / cz;
    *out_sz = cz;
}

static void draw_world_3d(
    surface_t* disp,
    const KengineWorldMesh* mesh,
    int cam_x, int cam_y, int cam_z,
    int yaw, int pitch,
    int proj_dist
) {
    if (!mesh || mesh->vertex_count > WORLD3D_MAX_VERTICES) return;

    int yaw_cos = w3d_cos(yaw), yaw_sin = w3d_sin(yaw);
    int pitch_cos = w3d_cos(pitch), pitch_sin = w3d_sin(pitch);
    int center_x = FB_WIDTH / 2, center_y = FB_HEIGHT / 2;
    int vc = mesh->vertex_count;
    const int* verts = mesh->vertices;
    int depthMin = 0x7FFFFFFF, depthMax = 0;

    for (int vi = 0; vi < vc; vi++) {
        int base = vi * mesh->vertex_stride;
        int rx = verts[base] - cam_x;
        int ry = verts[base + 1] - cam_y;
        int rz = verts[base + 2] - cam_z;

        int vx = (rx * yaw_cos - rz * yaw_sin) / WORLD3D_TRIG_SCALE;
        int yz = (rx * yaw_sin + rz * yaw_cos) / WORLD3D_TRIG_SCALE;
        int vy = (ry * pitch_cos - yz * pitch_sin) / WORLD3D_TRIG_SCALE;
        int vz = (ry * pitch_sin + yz * pitch_cos) / WORLD3D_TRIG_SCALE;

        w3d_view_x[vi] = vx;
        w3d_view_y[vi] = vy;
        w3d_view_z[vi] = vz;

        if (vz <= WORLD3D_NEAR_PLANE) {
            w3d_visible[vi] = 0;
            continue;
        }

        int sx = center_x + (vx * proj_dist + vz / 2) / vz;
        int sy = center_y - (vy * proj_dist + vz / 2) / vz;

        if (sx < -WORLD3D_COORD_LIMIT || sx > WORLD3D_COORD_LIMIT ||
            sy < -WORLD3D_COORD_LIMIT || sy > WORLD3D_COORD_LIMIT) {
            w3d_visible[vi] = 0;
            continue;
        }
        w3d_screen_x[vi] = sx;
        w3d_screen_y[vi] = sy;
        w3d_screen_z[vi] = vz;
        w3d_visible[vi] = 1;
        if (vz < depthMin) depthMin = vz;
        if (vz > depthMax) depthMax = vz;
    }

    int tc = mesh->triangle_count;
    const int* tris = mesh->triangles;
    const int* colors = mesh->colors;
    int drawn = 0;
    int depthSpan = depthMax - depthMin;
    if (depthSpan < 1) depthSpan = 1;

    /* Bucket sort: assign each visible triangle to a depth bucket */
    for (int i = 0; i < WORLD3D_DEPTH_BUCKETS; i++) {
        w3d_bucket_head[i] = -1;
        w3d_bucket_tail[i] = -1;
    }
    int vis_count = 0;

    for (int ti = 0; ti < tc && vis_count < WORLD3D_MAX_VISIBLE; ti++) {
        int tb = ti * 4;
        int a = tris[tb], b = tris[tb + 1], c = tris[tb + 2];
        int va = w3d_visible[a], vb = w3d_visible[b], vc2 = w3d_visible[c];
        int behind = (!va) + (!vb) + (!vc2);
        if (behind == 3) continue;
        if (behind > 0 && (w3d_view_z[a] < -200 || w3d_view_z[b] < -200 || w3d_view_z[c] < -200)) continue;

        int ax, ay, bx, by, cx, cy;
        int az_depth, bz_depth, cz_depth;

        if (behind == 0) {
            ax = w3d_screen_x[a]; ay = w3d_screen_y[a]; az_depth = w3d_screen_z[a];
            bx = w3d_screen_x[b]; by = w3d_screen_y[b]; bz_depth = w3d_screen_z[b];
            cx = w3d_screen_x[c]; cy = w3d_screen_y[c]; cz_depth = w3d_screen_z[c];
        } else {
            if (va) { ax = w3d_screen_x[a]; ay = w3d_screen_y[a]; az_depth = w3d_screen_z[a]; }
            else { w3d_clip_project(w3d_view_x[a], w3d_view_y[a], w3d_view_z[a],
                       w3d_view_x[va ? a : (vb ? b : c)], w3d_view_y[va ? a : (vb ? b : c)], w3d_view_z[va ? a : (vb ? b : c)],
                       proj_dist, center_x, center_y, &ax, &ay, &az_depth);
                   /* find a visible neighbor to clip toward */
                   int nb = vb ? b : (vc2 ? c : a);
                   w3d_clip_project(w3d_view_x[a], w3d_view_y[a], w3d_view_z[a],
                       w3d_view_x[nb], w3d_view_y[nb], w3d_view_z[nb],
                       proj_dist, center_x, center_y, &ax, &ay, &az_depth);
                 }
            if (vb) { bx = w3d_screen_x[b]; by = w3d_screen_y[b]; bz_depth = w3d_screen_z[b]; }
            else { int nb = va ? a : (vc2 ? c : b);
                   w3d_clip_project(w3d_view_x[b], w3d_view_y[b], w3d_view_z[b],
                       w3d_view_x[nb], w3d_view_y[nb], w3d_view_z[nb],
                       proj_dist, center_x, center_y, &bx, &by, &bz_depth);
                 }
            if (vc2) { cx = w3d_screen_x[c]; cy = w3d_screen_y[c]; cz_depth = w3d_screen_z[c]; }
            else { int nb = va ? a : (vb ? b : c);
                   w3d_clip_project(w3d_view_x[c], w3d_view_y[c], w3d_view_z[c],
                       w3d_view_x[nb], w3d_view_y[nb], w3d_view_z[nb],
                       proj_dist, center_x, center_y, &cx, &cy, &cz_depth);
                 }
        }

        int area = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
        if (w3d_abs(area) <= WORLD3D_DEGENERATE_AREA) continue;
        if ((ax < -WORLD3D_OFFSCREEN && bx < -WORLD3D_OFFSCREEN && cx < -WORLD3D_OFFSCREEN) ||
            (ax > FB_WIDTH + WORLD3D_OFFSCREEN && bx > FB_WIDTH + WORLD3D_OFFSCREEN && cx > FB_WIDTH + WORLD3D_OFFSCREEN) ||
            (ay < -WORLD3D_OFFSCREEN && by < -WORLD3D_OFFSCREEN && cy < -WORLD3D_OFFSCREEN) ||
            (ay > FB_HEIGHT + WORLD3D_OFFSCREEN && by > FB_HEIGHT + WORLD3D_OFFSCREEN && cy > FB_HEIGHT + WORLD3D_OFFSCREEN))
            continue;

        int avg_z = (az_depth + bz_depth + cz_depth) / 3;
        int clamped = w3d_clamp(avg_z - depthMin, 0, depthSpan);
        int bucket = (clamped * (WORLD3D_DEPTH_BUCKETS - 1)) / depthSpan;
        bucket = w3d_clamp(bucket, 0, WORLD3D_DEPTH_BUCKETS - 1);

        w3d_vis_tri[vis_count] = ti;
        w3d_vis_area[vis_count] = area;
        w3d_bucket_next[vis_count] = -1;
        int tail = w3d_bucket_tail[bucket];
        if (tail >= 0) {
            w3d_bucket_next[tail] = vis_count;
        } else {
            w3d_bucket_head[bucket] = vis_count;
        }
        w3d_bucket_tail[bucket] = vis_count;
        vis_count++;
    }

    /* Build material→texture lookup */
    surface_t tex_surfaces[32];
    int tex_mat_map[32];
    int tex_surface_count = 0;
    memset(tex_mat_map, -1, sizeof(tex_mat_map));

#if KENGINE_N64_USE_RDPQ_RENDER
    if (mesh->textures && mesh->texture_count > 0) {
        for (int ti2 = 0; ti2 < mesh->texture_count && tex_surface_count < 32; ti2++) {
            const KengineWorldTexture* wt = &mesh->textures[ti2];
            tex_surfaces[tex_surface_count] = surface_make_linear(
                (void*)wt->data, FMT_RGBA16, wt->width, wt->height
            );
            if (wt->material_index >= 0 && wt->material_index < 32) {
                tex_mat_map[wt->material_index] = tex_surface_count;
            }
            tex_surface_count++;
        }
    }
#endif

    /* Draw from farthest bucket to nearest — painter's algorithm */
#if KENGINE_N64_USE_RDPQ_RENDER
    rdpq_begin(disp);
    int current_tex = -1;
    int using_texture = 0;
#endif

    int vstride = mesh->vertex_stride;

    for (int bi = WORLD3D_DEPTH_BUCKETS - 1; bi >= 0; bi--) {
        int vi = w3d_bucket_head[bi];
        while (vi >= 0) {
            int ti = w3d_vis_tri[vi];
            int area = w3d_vis_area[vi];
            int tb = ti * 4;
            int a = tris[tb], b = tris[tb + 1], c = tris[tb + 2], ci = tris[tb + 3];
            int ax = w3d_screen_x[a], ay = w3d_screen_y[a];
            int bx = w3d_screen_x[b], by = w3d_screen_y[b];
            int cx = w3d_screen_x[c], cy = w3d_screen_y[c];

#if KENGINE_N64_USE_RDPQ_RENDER
            int tex_idx = (ci >= 0 && ci < 32) ? tex_mat_map[ci] : -1;

            if (tex_idx >= 0 && vstride >= 5) {
                if (tex_idx != current_tex || !using_texture) {
                    rdpq_sync_pipe();
                    rdpq_set_mode_standard();
                    rdpq_mode_combiner(RDPQ_COMBINER1((TEX0, 0, PRIM, 0), (TEX0, 0, PRIM, 0)));
                    const KengineWorldTexture* wt = &mesh->textures[tex_idx];
                    rdpq_tex_upload(TILE0, &tex_surfaces[tex_idx], NULL);
                    current_tex = tex_idx;
                    using_texture = 1;
                    g_rdpq_mode = KENGINE_RDPQ_MODE_NONE;
                }

                int avg_y = (ay + by + cy) / 3;
                int light = 80 + w3d_clamp((center_y - avg_y) / 3, -30, 40)
                               + w3d_clamp(w3d_abs(area) / 200, 0, 40)
                               + (area >= 0 ? 12 : -12);
                int shade = w3d_clamp(light, 40, 255);
                rdpq_set_prim_color(RGBA32(shade, shade, shade, 255));

                int ab = a * vstride, bb2 = b * vstride, cb = c * vstride;
                const int* vdata = mesh->vertices;
                float s0 = (float)vdata[ab + 3] / 32.0f, t0 = (float)vdata[ab + 4] / 32.0f;
                float s1 = (float)vdata[bb2 + 3] / 32.0f, t1 = (float)vdata[bb2 + 4] / 32.0f;
                float s2 = (float)vdata[cb + 3] / 32.0f, t2 = (float)vdata[cb + 4] / 32.0f;

                float v1[] = { (float)ax, (float)ay, s0, t0, 1.0f };
                float v2[] = { (float)bx, (float)by, s1, t1, 1.0f };
                float v3[] = { (float)cx, (float)cy, s2, t2, 1.0f };
                rdpq_triangle(&TRIFMT_TEX, v1, v2, v3);
            } else {
                if (using_texture) {
                    rdpq_sync_pipe();
                    rdpq_prepare_flat_triangle();
                    using_texture = 0;
                    current_tex = -1;
                }

                int avg_y = (ay + by + cy) / 3;
                int light = 80 + w3d_clamp((center_y - avg_y) / 3, -30, 40)
                               + w3d_clamp(w3d_abs(area) / 200, 0, 40)
                               + (area >= 0 ? 12 : -12);
                int shade = w3d_clamp(light, 40, 255);

                int base_color = (ci >= 0 && ci < mesh->color_count) ? colors[ci] : 0xFFB4B4B4;
                int sr = ((base_color & 0xFF) * shade) / 255;
                int sg = (((base_color >> 8) & 0xFF) * shade) / 255;
                int sb = (((base_color >> 16) & 0xFF) * shade) / 255;
                int sa = (base_color >> 24) & 0xFF;
                int final_color = sr | (sg << 8) | (sb << 16) | (sa << 24);

                rdpq_set_prim_color(kengine_rgba_to_rdpq_color(final_color));
                float v1[] = { (float)ax, (float)ay };
                float v2[] = { (float)bx, (float)by };
                float v3[] = { (float)cx, (float)cy };
                rdpq_triangle(&TRIFMT_FILL, v1, v2, v3);
            }
#else
            int avg_y = (ay + by + cy) / 3;
            int light = 80 + w3d_clamp((center_y - avg_y) / 3, -30, 40)
                           + w3d_clamp(w3d_abs(area) / 200, 0, 40)
                           + (area >= 0 ? 12 : -12);
            int shade = w3d_clamp(light, 40, 255);

            int base_color = (ci >= 0 && ci < mesh->color_count) ? colors[ci] : 0xFFB4B4B4;
            int sr = ((base_color & 0xFF) * shade) / 255;
            int sg = (((base_color >> 8) & 0xFF) * shade) / 255;
            int sb = (((base_color >> 16) & 0xFF) * shade) / 255;
            int sa = (base_color >> 24) & 0xFF;
            int final_color = sr | (sg << 8) | (sb << 16) | (sa << 24);
            draw_triangle(disp, ax, ay, bx, by, cx, cy, final_color);
#endif
            drawn++;
            vi = w3d_bucket_next[vi];
        }
    }

#if KENGINE_N64_USE_RDPQ_RENDER
    rdpq_flush();
#endif
}

#endif /* KENGINE_N64_WORLD_MESH */

static void draw_perf_overlay_line(surface_t* disp, const char* text, int line, uint32_t color) {
    draw_text(
        disp,
        text,
        KENGINE_PERF_OVERLAY_X,
        KENGINE_PERF_OVERLAY_Y + line * KENGINE_PERF_OVERLAY_LINE_HEIGHT,
        color,
        1
    );
}

static void draw_perf_overlay(
    surface_t* disp,
    int frame,
    int step_count,
    int render_count,
    int render_dropped,
    int audio_count,
    int audio_dropped,
    KengineFrameTiming timing
) {
    char buf[80];
    uint32_t color = graphics_make_color(0x9A, 0xF0, 0xB8, 0xC0);
    int fps_x10 = (int)(display_get_fps() * 10.0f + 0.5f);
    int fps = (fps_x10 + 5) / 10;
    heap_stats_t heap;
    sys_get_heap_stats(&heap);
    unsigned long heap_used_k = (unsigned long)(heap.used / 1024);
    unsigned long heap_total_k = (unsigned long)(heap.total / 1024);

    snprintf(
        buf,
        sizeof(buf),
        "FPS=%d CMD=%d+%d K=%d R=%d",
        fps,
        render_count,
        render_dropped,
        us_to_ms_rounded(timing.kotlin_us),
        us_to_ms_rounded(timing.render_us)
    );
    draw_perf_overlay_line(disp, buf, 0, color);

    snprintf(
        buf,
        sizeof(buf),
        "F=%d H=%lu/%luK",
        frame,
        heap_used_k,
        heap_total_k
    );
    draw_perf_overlay_line(disp, buf, 1, color);
}

static void draw_vertical_gradient(surface_t* disp, int top_color, int bottom_color, int pulse) {
    (void)pulse;

    uint8_t tr = top_color & 0xFF;
    uint8_t tg = (top_color >> 8) & 0xFF;
    uint8_t tb = (top_color >> 16) & 0xFF;
    uint8_t br = bottom_color & 0xFF;
    uint8_t bg = (bottom_color >> 8) & 0xFF;
    uint8_t bb = (bottom_color >> 16) & 0xFF;

    for (int y = 0; y < FB_HEIGHT; ++y) {
        uint8_t r = tr + (br - tr) * y / FB_HEIGHT;
        uint8_t g = tg + (bg - tg) * y / FB_HEIGHT;
        uint8_t b = tb + (bb - tb) * y / FB_HEIGHT;
        uint32_t c = graphics_make_color(r, g, b, 0xFF);
        graphics_draw_line(disp, 0, y, FB_WIDTH - 1, y, c);
    }
}

#if KENGINE_N64_USE_RDPQ_RENDER
static void draw_vertical_gradient_rdpq(surface_t* disp, int top_color, int bottom_color, int pulse) {
    (void)pulse;

    uint8_t tr = top_color & 0xFF;
    uint8_t tg = (top_color >> 8) & 0xFF;
    uint8_t tb = (top_color >> 16) & 0xFF;
    uint8_t br = bottom_color & 0xFF;
    uint8_t bg = (bottom_color >> 8) & 0xFF;
    uint8_t bb = (bottom_color >> 16) & 0xFF;

    const int band_height = 12;
    for (int y = 0; y < FB_HEIGHT; y += band_height) {
        int y1 = y + band_height;
        if (y1 > FB_HEIGHT) y1 = FB_HEIGHT;
        uint8_t r = tr + (br - tr) * y / FB_HEIGHT;
        uint8_t g = tg + (bg - tg) * y / FB_HEIGHT;
        uint8_t b = tb + (bb - tb) * y / FB_HEIGHT;
        rdpq_begin(disp);
        rdpq_prepare_fill(RGBA32(r, g, b, 0xFF));
        rdpq_fill_rectangle(0, y, FB_WIDTH, y1);
    }
}
#endif

#ifdef KENGINE_N64_SPRITE_ASSETS
static void draw_sprite(surface_t* disp, int sprite_id, int x, int y, int width, int height, int tint, int frame) {
    (void)tint;

    const KengineN64SpriteAsset* asset = kengine_n64_find_sprite_asset(sprite_id);
    if (!asset) {
        draw_rect(disp, x, y, width > 0 ? width : 16, height > 0 ? height : 16, 0xFFFF00FF);
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

    for (int dy = 0; dy < draw_h; ++dy) {
        int sample_y = src_y + ((dy * tile_h) / draw_h);
        for (int dx = 0; dx < draw_w; ++dx) {
            int sample_x = src_x + ((dx * tile_w) / draw_w);
            int src_pixel = sample_y * src_stride + sample_x;
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

static int translate_input(joypad_inputs_t inputs, joypad_buttons_t held, joypad_buttons_t pressed) {
    int mask = 0;

    if (held.d_left || pressed.d_left || inputs.stick_x < -STICK_DEADZONE) mask |= KENGINE_INPUT_LEFT;
    if (held.d_right || pressed.d_right || inputs.stick_x > STICK_DEADZONE) mask |= KENGINE_INPUT_RIGHT;
    if (held.d_up || pressed.d_up || inputs.stick_y > STICK_DEADZONE) mask |= KENGINE_INPUT_UP;
    if (held.d_down || pressed.d_down || inputs.stick_y < -STICK_DEADZONE) mask |= KENGINE_INPUT_DOWN;
    if (held.a || pressed.a) mask |= KENGINE_INPUT_A;
    if (held.b || pressed.b) mask |= KENGINE_INPUT_B;
    if (held.start || pressed.start) mask |= KENGINE_INPUT_START;
    if (held.c_up || pressed.c_up) mask |= KENGINE_INPUT_C_UP;
    if (held.c_down || pressed.c_down) mask |= KENGINE_INPUT_C_DOWN;
    if (held.c_left || pressed.c_left) mask |= KENGINE_INPUT_C_LEFT;
    if (held.c_right || pressed.c_right) mask |= KENGINE_INPUT_C_RIGHT;
    if (held.l || pressed.l) mask |= KENGINE_INPUT_L;
    if (held.r || pressed.r) mask |= KENGINE_INPUT_R;
    if (held.z || pressed.z) mask |= KENGINE_INPUT_Z;

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
#if KENGINE_N64_USE_RDPQ_RENDER
                clear_rdpq(disp, color);
#else
                uint32_t c = kengine_rgba_to_color(color);
                graphics_fill_screen(disp, c);
#endif
                break;
            }
            case KENGINE_RENDER_FILL_RECT:
#if KENGINE_N64_USE_RDPQ_RENDER
                draw_rect_rdpq(disp, x, y, w, h, color);
#else
                draw_rect(disp, x, y, w, h, color);
#endif
                break;
            case KENGINE_RENDER_VERTICAL_GRADIENT:
#if KENGINE_N64_USE_RDPQ_RENDER
                draw_vertical_gradient_rdpq(disp, color, color2, param);
#else
                draw_vertical_gradient(disp, color, color2, param);
#endif
                break;
            case KENGINE_RENDER_DRAW_LINE:
#if KENGINE_N64_USE_RDPQ_RENDER
                rdpq_flush();
#endif
                draw_line(disp, x, y, w, h, color);
                break;
            case KENGINE_RENDER_DRAW_TRIANGLE:
#if KENGINE_N64_USE_RDPQ_RENDER
                draw_triangle_rdpq(disp, x, y, w, h, color2, param, color);
#else
                draw_triangle(disp, x, y, w, h, color2, param, color);
#endif
                break;
            case KENGINE_RENDER_DRAW_WORLD_3D: {
#ifdef KENGINE_N64_WORLD_MESH
                int cam_x = x, cam_y = y, cam_z = w;
                int cam_yaw = h, cam_pitch = color;
                int mesh_id = color2;
                int proj_dist = param;
#if KENGINE_N64_USE_RDPQ_RENDER
                rdpq_flush();
#endif
                const KengineWorldMesh* mesh = kengine_find_world_mesh(mesh_id);
                if (mesh) {
                    draw_world_3d(disp, mesh, cam_x, cam_y, cam_z, cam_yaw, cam_pitch, proj_dist);
                }
#endif
                break;
            }
            case KENGINE_RENDER_DRAW_SPRITE:
#if KENGINE_N64_USE_RDPQ_RENDER
                rdpq_flush();
#endif
#ifdef KENGINE_N64_SPRITE_ASSETS
                draw_sprite(disp, color2, x, y, w, h, color, param);
#else
                draw_rect(disp, x, y, w > 0 ? w : 16, h > 0 ? h : 16, 0xFFFF00FF);
#endif
                break;
            case KENGINE_RENDER_DRAW_TEXT: {
#if KENGINE_N64_USE_RDPQ_RENDER
                rdpq_flush();
#endif
#ifndef KENGINE_N64_C_ONLY
                const char* text = (const char*)kengine_n64_kotlin_kengineN64RuntimeCommandText(i);
                int scale = param > 0 ? param : 1;
                draw_text_rgba(disp, text, x, y, color, scale);
                kengine_n64_kotlin_DisposeString(text);
#endif
                break;
            }
            default:
                break;
        }
    }
#if KENGINE_N64_USE_RDPQ_RENDER
    rdpq_flush();
#endif
}

#ifdef KENGINE_N64_C_ONLY

int main(void) {
    display_init(RESOLUTION_320x240, DEPTH_16_BPP, 3, GAMMA_NONE, FILTERS_RESAMPLE);
#if KENGINE_N64_USE_RDPQ_RENDER
    rdpq_init();
#endif
    joypad_init();
    timer_init();

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

static int kotlin_runtime_step(
    int host_frame,
    int input_mask
) {
    return kengine_n64_kotlin_kengineN64RuntimeStep(
        host_frame,
        input_mask,
        FB_WIDTH,
        FB_HEIGHT
    );
}

static int kotlin_copy_commands(int* destination, int max_commands) {
    return kengine_n64_kotlin_kengineN64RuntimeCopyCommands(destination, max_commands);
}

static int kotlin_copy_audio_commands(int* destination, int max_commands) {
    return kengine_n64_kotlin_kengineN64RuntimeCopyAudioCommands(destination, max_commands);
}

int main(void) {
    display_init(RESOLUTION_320x240, DEPTH_16_BPP, 3, GAMMA_NONE, FILTERS_RESAMPLE);
#if KENGINE_N64_USE_RDPQ_RENDER
    rdpq_init();
#endif
    joypad_init();
    timer_init();
#ifdef KENGINE_N64_SOUND_ASSETS
    kengine_audio_init();
#endif

    memset(g_storage_slots, 0, sizeof(g_storage_slots));

    disable_interrupts();
    kotlin_runtime_start();
    enable_interrupts();

    int frame = 0;

    while (1) {
        surface_t* disp = display_get();
        long long work_start = timer_ticks();
        KengineFrameTiming timing;
        memset(&timing, 0, sizeof(timing));

        joypad_poll();
        joypad_inputs_t inputs = joypad_get_inputs(JOYPAD_PORT_1);
        joypad_buttons_t held = joypad_get_buttons_held(JOYPAD_PORT_1);
        joypad_buttons_t pressed = joypad_get_buttons_pressed(JOYPAD_PORT_1);
        int input_mask = translate_input(inputs, held, pressed);

        long long phase_start = timer_ticks();
        int step_count = kotlin_runtime_step(frame, input_mask);
        int render_count = 0;
        int render_dropped = 0;
        int audio_count = 0;
        int audio_dropped = 0;
        if (step_count >= 0) {
            render_count = kotlin_copy_commands(g_render_commands, KENGINE_RENDER_MAX_COMMANDS);
            render_dropped = kengine_n64_kotlin_kengineN64RuntimeDroppedRenderCommands();
            audio_count = kotlin_copy_audio_commands(g_audio_commands, KENGINE_AUDIO_MAX_COMMANDS);
            audio_dropped = kengine_n64_kotlin_kengineN64RuntimeDroppedAudioCommands();
        }
        long long phase_end = timer_ticks();
        timing.kotlin_us = ticks_to_us(phase_end - phase_start);

        phase_start = phase_end;
        execute_render_commands(disp, g_render_commands, render_count);
        phase_end = timer_ticks();
        timing.render_us = ticks_to_us(phase_end - phase_start);

        phase_start = phase_end;
#ifdef KENGINE_N64_SOUND_ASSETS
        if (audio_count > 0) {
            execute_audio_commands(g_audio_commands, audio_count);
        }
#endif
        phase_end = timer_ticks();
        timing.native_audio_us = ticks_to_us(phase_end - phase_start);

        timing.total_us = ticks_to_us(timer_ticks() - work_start);
        draw_perf_overlay(
            disp,
            frame,
            step_count,
            render_count,
            render_dropped,
            audio_count,
            audio_dropped,
            timing
        );

        display_show(disp);
        frame++;
    }

    const char* cleanup_message = kengine_n64_kotlin_kengineN64RuntimeCleanup();
    kengine_n64_kotlin_DisposeString(cleanup_message);
    return 0;
}

#endif
