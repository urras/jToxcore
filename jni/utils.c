#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifdef WIN32
#include <winsock2.h>
#include <windows.h>
#else
#include <arpa/inet.h>
#endif
#include <tox/tox.h>
#include <tox/toxav.h>
#include <jni.h>
#include "types.h"

#ifdef ANDROID
#define ATTACH_THREAD(ptr,env) (*ptr->jvm)->AttachCurrentThread(ptr->jvm, &env, 0)
#else
#define ATTACH_THREAD(ptr,env) (*ptr->jvm)->AttachCurrentThread(ptr->jvm, (void **) &env, 0)
#endif
#define ALIGN(x, y) y*((x + (y-1))/y)

ToxAvCSettings codec_settings_to_native(JNIEnv *env, jobject codec_settings)
{
	jclass clazz;
	jfieldID call_type_id;
	jfieldID video_bitrate_id;
	jfieldID max_video_width_id;
	jfieldID max_video_height_id;
	jfieldID audio_bitrate_id;
	jfieldID audio_frame_duration_id;
	jfieldID audio_sample_rate_id;
	jfieldID audio_channels_id;
	jobject *call_type_obj;
	jint *video_bitrate;
	jint *max_video_width;
	jint *max_video_height;
	jint *audio_bitrate;
	jint *audio_frame_duration;
	jint *audio_sample_rate;
	jint *audio_channels;
	ToxAvCallType call_type;
	jclass enum_class;
	jmethodID get_name_method;
	jstring enum_name;
	const char *enum_name_native;
	ToxAvCSettings codec_settings_native;

	//Get java class for struct, and its field ids
	clazz = (*env)->FindClass(env, "im/tox/jtoxcore/ToxCodecSettings");
	call_type_id = (*env)->GetFieldID(env, clazz, "call_type", "Lim/tox/jtoxcore/ToxCodecSettings");
	video_bitrate_id = (*env)->GetFieldID(env, clazz, "video_bitrate", "Lim/tox/jtoxcore/ToxCodecSettings");
	max_video_width_id = (*env)->GetFieldID(env, clazz, "max_video_width", "Lim/tox/jtoxcore/ToxCodecSettings");
	max_video_height_id = (*env)->GetFieldID(env, clazz, "max_video_height", "Lim/tox/jtoxcore/ToxCodecSettings");
	audio_bitrate_id = (*env)->GetFieldID(env, clazz, "audio_bitrate", "Lim/tox/jtoxcore/ToxCodecSettings");
	audio_frame_duration_id = (*env)->GetFieldID(env, clazz, "audio_frame_duration", "Lim/tox/jtoxcore/ToxCodecSettings");
	audio_sample_rate_id = (*env)->GetFieldID(env, clazz, "audio_sample_rate", "Lim/tox/jtoxcore/ToxCodecSettings");
	audio_channels_id = (*env)->GetFieldID(env, clazz, "audio_channels", "Lim/tox/jtoxcore/ToxCodecSettings");

	//Get calltype java enum from call settings class
	call_type_obj = (*env)->GetObjectField(env, codec_settings, call_type_id);
	//Get remaining class members
	video_bitrate = (*env)->GetObjectField(env, codec_settings, video_bitrate_id);
	max_video_width = (*env)->GetObjectField(env, codec_settings, max_video_width_id);
	max_video_height = (*env)->GetObjectField(env, codec_settings, max_video_height_id);
	audio_bitrate = (*env)->GetObjectField(env, codec_settings, audio_bitrate_id);
	audio_frame_duration = (*env)->GetObjectField(env, codec_settings, audio_frame_duration_id);
	audio_sample_rate = (*env)->GetObjectField(env, codec_settings, audio_sample_rate_id);
	audio_channels = (*env)->GetObjectField(env, codec_settings, audio_channels_id);

	//Turn calltype java enum into c enum
	enum_class = (*env)->FindClass(env, "im/tox/jtoxcore/ToxCallType");
	get_name_method = (*env)->GetMethodID(env, enum_class, "name", "()Ljava/lang/String;");
	enum_name = (jstring)(*env)->CallObjectMethod(env, call_type_obj, get_name_method);
	enum_name_native = (*env)->GetStringUTFChars(env, enum_name, 0);

	if (strcmp(enum_name_native, "TYPE_AUDIO") == 0) {
		call_type = TypeAudio;
	} else if (strcmp(enum_name_native, "TYPE_VIDEO") == 0) {
		call_type = TypeVideo;
	}

	codec_settings_native.call_type = call_type;
	codec_settings_native.video_bitrate = *video_bitrate;
	codec_settings_native.max_video_width = *max_video_width;
	codec_settings_native.max_video_height = *max_video_height;
	codec_settings_native.audio_bitrate = *audio_bitrate;
	codec_settings_native.audio_frame_duration = *audio_frame_duration;
	codec_settings_native.audio_sample_rate = *audio_sample_rate;
	codec_settings_native.audio_channels = *audio_channels;
	return codec_settings_native;
}

jobject codec_settings_to_java(JNIEnv *env, ToxAvCSettings codec_settings_native)
{
	jclass clazz;
	jclass enum_class;
	jfieldID enum_field_id;
	jobject *call_type;
	jmethodID init_method;

	//Turn calltype c enum into java enum
	enum_class = (*env)->FindClass(env, "im/tox/jtoxcore/ToxCallType");

	if (codec_settings_native.call_type == TypeAudio) {
		enum_field_id = (*env)->GetStaticFieldID(env, enum_class, "TYPE_AUDIO", "Lim/tox/jtoxcore/ToxCallType");
	} else if (codec_settings_native.call_type == TypeVideo) {
		enum_field_id = (*env)->GetStaticFieldID(env, enum_class, "TYPE_VIDEO", "Lim/tox/jtoxcore/ToxCallType");
	}

	call_type = (*env)->GetStaticObjectField(env, enum_class, enum_field_id);

	//Get java class for struct, and create java object
	clazz = (*env)->FindClass(env, "im/tox/jtoxcore/ToxCodecSettings");
	init_method = (*env)->GetMethodID(env, clazz, "<init>", "(Lim/tox/jtoxcore/ToxCallType;IIIIIII)V");
	jobject codec_settings = (*env)->NewObject(env, clazz, init_method
							 , *call_type
							 , (jint) codec_settings_native.video_bitrate
							 , (jint) codec_settings_native.max_video_width
							 , (jint) codec_settings_native.max_video_height
							 , (jint) codec_settings_native.audio_bitrate
							 , (jint) codec_settings_native.audio_frame_duration
							 , (jint) codec_settings_native.audio_sample_rate
							 , (jint) codec_settings_native.audio_channels
											  );
	return codec_settings;
}

void avcallback_helper(int32_t call_id, void *user_data, char *enum_name)
{
	tox_av_jni_globals_t *globals = (tox_av_jni_globals_t *) user_data;
	JNIEnv *env;
	jclass enum_class;
	jfieldID enum_field_id;
	jobject callback_id;

	ATTACH_THREAD(globals, env);

	//create java callback id enum
	enum_class = (*env)->FindClass(env, "im/tox/jtoxcore/ToxAvCallbackID");
	enum_field_id = (*env)->GetStaticFieldID(env, enum_class, enum_name, "Lim/tox/jtoxcore/ToxAvCallbackID;");
	callback_id = (*env)->GetStaticObjectField(env, enum_class, enum_field_id);

    (*env)->CallVoidMethod(env, globals->handler, globals->cache->onAvCallbackMethodId, call_id, callback_id);
}


