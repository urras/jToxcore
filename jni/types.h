typedef struct {
    Tox *tox;
    JavaVM *jvm;
    jobject handler;
    jobject jtox;
} tox_jni_globals_t;

typedef struct {
    ToxAv *toxav;
    JavaVM *jvm;
    jobject handler;
    jobject jtox;
} tox_av_jni_globals_t;
