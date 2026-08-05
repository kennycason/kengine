typedef unsigned int size_t;

extern "C" {

void* malloc(size_t size);
void free(void* ptr);
void* memcpy(void* dst, const void* src, size_t n);
void* memmove(void* dst, const void* src, size_t n);
void* memset(void* s, int c, size_t n);
void abort(void);

void* _Znwj(unsigned int size);
void _ZdlPv(void* ptr);

/* std::__cxx11::basic_string<char> (GCC ABI, 32-bit) */

struct __cxx11_string {
    char* _M_p;
    unsigned int _M_length;
    union {
        char _M_local_buf[16];
        unsigned int _M_allocated_capacity;
    };
};

static const unsigned int _S_local_capacity = 15;

static int _M_is_local(const __cxx11_string* s) {
    return s->_M_p == (const char*)&s->_M_local_buf[0];
}

static unsigned int _M_capacity(const __cxx11_string* s) {
    if (_M_is_local(s)) return _S_local_capacity;
    return s->_M_allocated_capacity;
}

char* _ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE9_M_createERjj(
    __cxx11_string* self, unsigned int* capacity, unsigned int old_capacity) {
    unsigned int requested = *capacity;
    if (requested > old_capacity && requested < 2 * old_capacity)
        requested = 2 * old_capacity;
    char* p = (char*)_Znwj(requested + 1);
    *capacity = requested;
    return p;
}

void _ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE9_M_mutateEjjPKcj(
    __cxx11_string* self, unsigned int pos, unsigned int len1,
    const char* s, unsigned int len2) {
    unsigned int old_len = self->_M_length;
    unsigned int new_len = old_len + len2 - len1;
    unsigned int cap = _M_capacity(self);

    if (new_len > cap) {
        unsigned int new_cap = new_len;
        char* new_data = _ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE9_M_createERjj(
            self, &new_cap, cap);
        if (pos > 0)
            memcpy(new_data, self->_M_p, pos);
        if (s && len2 > 0)
            memcpy(new_data + pos, s, len2);
        unsigned int suffix_len = old_len - pos - len1;
        if (suffix_len > 0)
            memcpy(new_data + pos + len2, self->_M_p + pos + len1, suffix_len);
        if (!_M_is_local(self))
            _ZdlPv(self->_M_p);
        self->_M_p = new_data;
        self->_M_allocated_capacity = new_cap;
    } else {
        unsigned int suffix_len = old_len - pos - len1;
        if (suffix_len > 0 && len1 != len2)
            memmove(self->_M_p + pos + len2, self->_M_p + pos + len1, suffix_len);
        if (s && len2 > 0)
            memcpy(self->_M_p + pos, s, len2);
    }
    self->_M_length = new_len;
    self->_M_p[new_len] = '\0';
}

void _ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE7reserveEj(
    __cxx11_string* self, unsigned int res) {
    unsigned int cap = _M_capacity(self);
    if (res <= cap) return;
    unsigned int new_cap = res;
    char* new_data = _ZNSt7__cxx1112basic_stringIcSt11char_traitsIcESaIcEE9_M_createERjj(
        self, &new_cap, cap);
    unsigned int len = self->_M_length;
    if (len > 0) memcpy(new_data, self->_M_p, len);
    new_data[len] = '\0';
    if (!_M_is_local(self)) _ZdlPv(self->_M_p);
    self->_M_p = new_data;
    self->_M_allocated_capacity = new_cap;
}

/* std::__detail::_List_node_base */

struct _List_node_base {
    _List_node_base* _M_next;
    _List_node_base* _M_prev;
};

void _ZNSt8__detail15_List_node_base7_M_hookEPS0_(
    _List_node_base* self, _List_node_base* position) {
    self->_M_next = position;
    self->_M_prev = position->_M_prev;
    position->_M_prev->_M_next = self;
    position->_M_prev = self;
}

void _ZNSt8__detail15_List_node_base11_M_transferEPS0_S1_(
    _List_node_base* self, _List_node_base* first, _List_node_base* last) {
    if (self == last) return;
    last->_M_prev->_M_next = self;
    first->_M_prev->_M_next = last;
    self->_M_prev->_M_next = first;
    _List_node_base* tmp = self->_M_prev;
    self->_M_prev = last->_M_prev;
    last->_M_prev = first->_M_prev;
    first->_M_prev = tmp;
}

/* std::_Rb_tree_node_base */

struct _Rb_tree_node_base {
    int _M_color;
    _Rb_tree_node_base* _M_parent;
    _Rb_tree_node_base* _M_left;
    _Rb_tree_node_base* _M_right;
};

const _Rb_tree_node_base* _ZSt18_Rb_tree_incrementPKSt18_Rb_tree_node_base(
    const _Rb_tree_node_base* __x) {
    _Rb_tree_node_base* x = const_cast<_Rb_tree_node_base*>(__x);
    if (x->_M_right != 0) {
        x = x->_M_right;
        while (x->_M_left != 0) x = x->_M_left;
    } else {
        _Rb_tree_node_base* y = x->_M_parent;
        while (x == y->_M_right) { x = y; y = y->_M_parent; }
        if (x->_M_right != y) x = y;
    }
    return x;
}

/* std::__detail::_Prime_rehash_policy::_M_need_rehash */

struct _Prime_rehash_policy {
    float _M_max_load_factor;
    unsigned int _M_next_resize;
};

struct _Rehash_result {
    unsigned int _M_first;
    unsigned int _M_second;
};

static const unsigned int _prime_list[] = {
    2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53,
    59, 67, 79, 89, 97, 107, 127, 137, 157, 179, 199, 227, 257, 293,
    331, 373, 421, 479, 541, 613, 691, 769, 863, 971, 1097, 1237, 1399,
    1579, 2003, 2269, 2557, 2897, 3259, 3673, 4139, 4673, 5279, 5953,
    6709, 7559, 8527, 9613, 10831, 12211, 13763, 15511, 17489, 19709,
    22193, 25013, 28183, 31769, 35803, 40361, 45481, 51239, 57749, 65089
};
static const int _n_primes = sizeof(_prime_list) / sizeof(_prime_list[0]);

static unsigned int _next_prime(unsigned int n) {
    for (int i = 0; i < _n_primes; i++)
        if (_prime_list[i] >= n) return _prime_list[i];
    return n | 1;
}

void _ZNKSt8__detail20_Prime_rehash_policy14_M_need_rehashEjjj(
    _Rehash_result* __ret,
    const _Prime_rehash_policy* self,
    unsigned int __n_bkt,
    unsigned int __n_elt,
    unsigned int __n_ins) {
    if (__n_elt + __n_ins > self->_M_next_resize) {
        float __min_bkts = ((float)(__n_elt + __n_ins)) / self->_M_max_load_factor;
        unsigned int __min_bkts_i = (unsigned int)__min_bkts;
        if ((float)__min_bkts_i < __min_bkts) __min_bkts_i++;
        if (__min_bkts_i > __n_bkt) {
            unsigned int new_bkt = _next_prime(__min_bkts_i);
            __ret->_M_first = 1;
            __ret->_M_second = new_bkt;
            ((_Prime_rehash_policy*)self)->_M_next_resize =
                (unsigned int)((float)new_bkt * self->_M_max_load_factor);
            return;
        }
    }
    __ret->_M_first = 0;
    __ret->_M_second = 0;
}

/* __gxx_personality_v0 */
int __gxx_personality_v0(int version, int actions,
                          unsigned long long exception_class,
                          void* exception_info, void* context) {
    (void)version; (void)actions; (void)exception_class;
    (void)exception_info; (void)context;
    return 3;
}

/* Float <-> Int64 conversions */
long long __fixdfdi(double a) {
    if (a != a) return 0;
    int neg = 0;
    if (a < 0) { a = -a; neg = 1; }
    unsigned long long acc = 0;
    if (a >= 1.0) {
        double pow2 = 1.0;
        int bits = 0;
        while (pow2 * 2.0 <= a && bits < 62) { pow2 *= 2.0; bits++; }
        for (int i = bits; i >= 0; i--) {
            if (a >= pow2) { acc |= (1ULL << i); a -= pow2; }
            pow2 /= 2.0;
        }
    }
    long long result = (long long)acc;
    return neg ? -result : result;
}

long long __fixsfdi(float a) { return __fixdfdi((double)a); }

double __floatdidf(long long a) {
    if (a == 0) return 0.0;
    int neg = 0;
    unsigned long long ua;
    if (a < 0) { neg = 1; ua = (unsigned long long)(-a); }
    else { ua = (unsigned long long)a; }
    double result = 0.0;
    double pow2 = 1.0;
    for (int i = 0; i < 64; i++) {
        if (ua & (1ULL << i)) result += pow2;
        pow2 *= 2.0;
    }
    return neg ? -result : result;
}

float __floatdisf(long long a) { return (float)__floatdidf(a); }

} /* extern "C" */
