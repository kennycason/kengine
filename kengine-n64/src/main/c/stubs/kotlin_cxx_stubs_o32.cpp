typedef unsigned int size_t;

extern "C" {
void* malloc(size_t size);
void free(void* ptr);
void* memcpy(void* dst, const void* src, size_t n);
void* memset(void* s, int c, size_t n);
void abort(void);

void* _Znwj(unsigned int size) {
    void* p = malloc(size);
    return p;
}

void _ZdlPv(void* ptr) { free(ptr); }
void _ZdlPvj(void* ptr, unsigned int) { free(ptr); }
void* _Znaj(unsigned int size) { return _Znwj(size); }
void _ZdaPv(void* ptr) { free(ptr); }
void _ZdaPvj(void* ptr, unsigned int) { free(ptr); }

typedef void (*terminate_handler)(void);
static terminate_handler current_terminate_handler = 0;

void _ZSt9terminatev(void) {
    if (current_terminate_handler) current_terminate_handler();
    abort();
}

terminate_handler _ZSt13set_terminatePFvvE(terminate_handler f) {
    terminate_handler old = current_terminate_handler;
    current_terminate_handler = f;
    return old;
}

int __cxa_guard_acquire(int* guard_object) {
    char* initialized = (char*)guard_object;
    if (*initialized) return 0;
    return 1;
}
void __cxa_guard_release(int* guard_object) {
    char* initialized = (char*)guard_object;
    *initialized = 1;
}
void __cxa_guard_abort(int* guard_object) { (void)guard_object; }

void* __cxa_allocate_exception(unsigned int size) { return malloc(size); }
void __cxa_throw(void* thrown_exception, void* tinfo, void (*dest)(void*)) {
    (void)thrown_exception; (void)tinfo; (void)dest;
    abort();
}
void __cxa_rethrow(void) { abort(); }
void* __cxa_begin_catch(void* exceptionObject) { return exceptionObject; }
void __cxa_end_catch(void) {}

void _ZSt17__throw_bad_allocv(void) { abort(); }
void _ZSt20__throw_length_errorPKc(const char* m) { (void)m; abort(); }
void _ZSt25__throw_bad_function_callv(void) { abort(); }
void _ZSt28__throw_bad_array_new_lengthv(void) { abort(); }

typedef int (*_Unwind_Trace_Fn)(void*, void*);
int _Unwind_Backtrace(_Unwind_Trace_Fn fn, void* arg) { (void)fn; (void)arg; return 0; }
unsigned long _Unwind_GetIP(void* context) { (void)context; return 0; }
void _Unwind_Resume(void* exception_object) { (void)exception_object; abort(); }

struct __exception_ptr_placeholder { int dummy; };
void* _ZSt17current_exceptionv(void) {
    static __exception_ptr_placeholder null_exception = {0};
    return &null_exception;
}
void _ZSt17rethrow_exceptionNSt15__exception_ptr13exception_ptrE(void* p) { (void)p; abort(); }
void _ZNSt15__exception_ptr13exception_ptr9_M_addrefEv(void* self) { (void)self; }
void _ZNSt15__exception_ptr13exception_ptr10_M_releaseEv(void* self) { (void)self; }

void _Exit(int status) { (void)status; while(1) {} }

void* _ZTVN10__cxxabiv117__class_type_infoE[4] = {0, 0, 0, 0};
void* _ZTVN10__cxxabiv120__si_class_type_infoE[4] = {0, 0, 0, 0};
void* _ZTVN10__cxxabiv121__vmi_class_type_infoE[4] = {0, 0, 0, 0};

unsigned long long get_ticks(void);

struct timespec_like { long long ns; };
timespec_like _ZNSt6chrono3_V212steady_clock3nowEv(void) {
    unsigned long long ticks = get_ticks();
    long long ns = (long long)(ticks * 21ULL + ticks / 3ULL);
    timespec_like tp;
    tp.ns = ns;
    return tp;
}

} /* extern "C" */
