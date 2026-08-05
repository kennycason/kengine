#pragma once

int kengine_n64_storage_load(const char* key, void* destination, int max_bytes);
int kengine_n64_storage_save(const char* key, const void* data, int size);
int kengine_n64_storage_delete(const char* key);
int kengine_n64_storage_exists(const char* key);
