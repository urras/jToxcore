typedef struct {
   jmethodID onFileControlMethodId;
   jmethodID onFileDataMethodId;
   jmethodID onFileSendRequestMethodId;
   jmethodID onFriendRequestMethodId;
   jmethodID onMessageMethodId;
   jmethodID onActionMethodId;
   jmethodID onNameChangeMethodId;
   jmethodID onStatusMessageMethodId;
   jmethodID onUserStatusMethodId;
   jmethodID onReadReceiptMethodId;
   jmethodID onConnectionStatusMethodId;
   jmethodID onTypingChangeMethodId;
   jmethodID onAudioDataMethodId;
   jmethodID onVideoDataMethodId;
   jmethodID onAvCallbackMethodId;
} cachedId;

typedef struct {
    Tox *tox;
    JavaVM *jvm;
    jobject handler;
    jobject jtox;
    cachedId *cache;
} tox_jni_globals_t;

typedef struct {
    ToxAv *toxav;
    JavaVM *jvm;
    jobject handler;
    jobject jtox;
    cachedId *cache;
} tox_av_jni_globals_t;
