typedef unsigned int size_t;
typedef int ssize_t;

extern void* malloc(size_t size);
extern void free(void* ptr);
extern void* memalign(size_t alignment, size_t size);
extern void* memcpy(void* dst, const void* src, size_t n);
extern void* memset(void* s, int c, size_t n);

unsigned int sleep(unsigned int seconds) {
    (void)seconds;
    return 0;
}

int posix_memalign(void** memptr, size_t alignment, size_t size) {
    if (!memptr) return -1;
    if (alignment < sizeof(void*)) alignment = sizeof(void*);
    if ((alignment & (alignment - 1)) != 0) return -1;

    void* ptr = memalign(alignment, size);
    if (!ptr) return -1;
    *memptr = ptr;
    return 0;
}

ssize_t write(int fd, const void* buf, size_t count) {
    (void)fd;
    (void)buf;
    return (ssize_t)count;
}

unsigned long long __atomic_load_8(const volatile void* ptr, int memorder) {
    (void)memorder;
    unsigned long long val;
    memcpy(&val, (const void*)ptr, 8);
    return val;
}

void __atomic_store_8(volatile void* ptr, unsigned long long val, int memorder) {
    (void)memorder;
    memcpy((void*)ptr, &val, 8);
}

unsigned long long __atomic_exchange_8(volatile void* ptr, unsigned long long val, int memorder) {
    (void)memorder;
    unsigned long long old;
    memcpy(&old, (void*)ptr, 8);
    memcpy((void*)ptr, &val, 8);
    return old;
}

int __atomic_compare_exchange_8(volatile void* ptr, void* expected,
                                unsigned long long desired,
                                int weak, int success_memorder, int failure_memorder) {
    (void)weak; (void)success_memorder; (void)failure_memorder;
    unsigned long long current;
    memcpy(&current, (void*)ptr, 8);
    unsigned long long exp;
    memcpy(&exp, expected, 8);
    if (current == exp) {
        memcpy((void*)ptr, &desired, 8);
        return 1;
    } else {
        memcpy(expected, &current, 8);
        return 0;
    }
}

unsigned long long __atomic_fetch_add_8(volatile void* ptr, unsigned long long val, int memorder) {
    (void)memorder;
    unsigned long long old;
    memcpy(&old, (void*)ptr, 8);
    unsigned long long new_val = old + val;
    memcpy((void*)ptr, &new_val, 8);
    return old;
}

unsigned long long __atomic_fetch_sub_8(volatile void* ptr, unsigned long long val, int memorder) {
    (void)memorder;
    unsigned long long old;
    memcpy(&old, (void*)ptr, 8);
    unsigned long long new_val = old - val;
    memcpy((void*)ptr, &new_val, 8);
    return old;
}

long long __divdi3(long long a, long long b) {
    if (b == 0) return 0;
    int neg = 0;
    unsigned long long ua, ub;
    if (a < 0) { ua = (unsigned long long)(-a); neg = 1; } else { ua = (unsigned long long)a; }
    if (b < 0) { ub = (unsigned long long)(-b); neg ^= 1; } else { ub = (unsigned long long)b; }
    unsigned long long q = 0, r = 0;
    for (int i = 63; i >= 0; i--) {
        r = (r << 1) | ((ua >> i) & 1);
        if (r >= ub) { r -= ub; q |= (1ULL << i); }
    }
    return neg ? -(long long)q : (long long)q;
}

unsigned long long __udivdi3(unsigned long long a, unsigned long long b) {
    if (b == 0) return 0;
    unsigned long long q = 0, r = 0;
    for (int i = 63; i >= 0; i--) {
        r = (r << 1) | ((a >> i) & 1);
        if (r >= b) { r -= b; q |= (1ULL << i); }
    }
    return q;
}

long long __moddi3(long long a, long long b) {
    return a - __divdi3(a, b) * b;
}

unsigned long long __umoddi3(unsigned long long a, unsigned long long b) {
    return a - __udivdi3(a, b) * b;
}

typedef struct {
    unsigned long size;
    unsigned long align;
    unsigned long ptr;
    void* templ;
} __emutls_control;

void* __emutls_get_address(__emutls_control* control) {
    if (control->ptr == 0) {
        void* p = malloc(control->size);
        if (control->templ) memcpy(p, control->templ, control->size);
        else memset(p, 0, control->size);
        control->ptr = (unsigned long)p;
    }
    return (void*)control->ptr;
}
