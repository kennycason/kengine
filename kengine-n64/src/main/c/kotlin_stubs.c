#include <stdlib.h>
#include <string.h>
#include <malloc.h>

unsigned int sleep(unsigned int seconds) {
    (void)seconds;
    return 0;
}

int posix_memalign(void** memptr, size_t alignment, size_t size) {
    void* p = memalign(alignment, size);
    if (!p) return -1;
    *memptr = p;
    return 0;
}
