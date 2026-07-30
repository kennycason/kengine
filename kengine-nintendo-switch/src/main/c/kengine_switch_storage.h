#pragma once

int kengine_switch_storage_load(const char* key, void* destination, int max_bytes);
int kengine_switch_storage_save(const char* key, const void* data, int size);
int kengine_switch_storage_delete(const char* key);
int kengine_switch_storage_exists(const char* key);
