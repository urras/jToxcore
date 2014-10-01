/* JTox.c
 *
 *  Copyright (C) 2014 Tox project All Rights Reserved.
 *
 *  This file is part of jToxcore
 *
 *  jToxcore is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  jToxcore is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with jToxcore.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
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

#include "JTox.h"
#include "callbacks.h"
#include "types.h"
#include "utils.h"

#define ADDR_SIZE_HEX (TOX_FRIEND_ADDRESS_SIZE * 2 + 1)
#define UNUSED(x) (void)(x)

#ifdef ANDROID
#define ATTACH_THREAD(ptr,env) (*ptr->jvm)->AttachCurrentThread(ptr->jvm, &env, 0)
#else
#define ATTACH_THREAD(ptr,env) (*ptr->jvm)->AttachCurrentThread(ptr->jvm, (void **) &env, 0)
#endif

#define ALIGN(x, y) y*((x + (y-1))/y)
/**
 * Begin Utilities section
 */
cachedId* cache;
Tox_Options tox_options_to_native(JNIEnv *env, jobject tox_options);
/**
 * Convert a given binary address to a human-readable, \0-terminated hexadecimal string
 */
void addr_to_hex(uint8_t *addr, char *buf)
{
	uint32_t i;

	for (i = 0; i < TOX_FRIEND_ADDRESS_SIZE; i++) {
		char xx[3];
		snprintf(xx, sizeof(xx), "%02X", addr[i] & 0xff);
		strcat(buf, xx);
	}
}

/**
 * End Utilities section
 */



/**
 * Begin maintenance section
 */

jint JNI_OnLoad(JavaVM* jvm, void* aReserved)
{
    cache = malloc(sizeof(cachedId));
    JNIEnv *env;


    #ifdef ANDROID
        (*jvm)->AttachCurrentThread(jvm, &env, 0);
    #else
        (*jvm)->AttachCurrentThread(jvm, (void **) &env, 0);
    #endif

    jclass handlerclass = (*env)->FindClass(env, "im/tox/jtoxcore/callbacks/CallbackHandler");

    cache->onFileControlMethodId = (*env)->GetMethodID(env, handlerclass, "onFileControl",
                                      "(IIILim/tox/jtoxcore/ToxFileControl;[B)V");
    cache->onFileDataMethodId = (*env)->GetMethodID(env, handlerclass, "onFileData", "(II[B)V");
    cache->onFileSendRequestMethodId = (*env)->GetMethodID(env, handlerclass, "onFileSendRequest", "(IIJ[B)V");
    cache->onFriendRequestMethodId = (*env)->GetMethodID(env, handlerclass, "onFriendRequest", "(Ljava/lang/String;[B)V");
    cache->onMessageMethodId = (*env)->GetMethodID(env, handlerclass, "onMessage", "(I[B)V");
    cache->onActionMethodId = (*env)->GetMethodID(env, handlerclass, "onAction", "(I[B)V");
    cache->onNameChangeMethodId = (*env)->GetMethodID(env, handlerclass, "onNameChange", "(I[B)V");
    cache->onStatusMessageMethodId = (*env)->GetMethodID(env, handlerclass, "onStatusMessage", "(I[B)V");
    cache->onUserStatusMethodId = (*env)->GetMethodID(env, handlerclass, "onUserStatus",
                                                      "(ILim/tox/jtoxcore/ToxUserStatus;)V");
    cache->onReadReceiptMethodId = (*env)->GetMethodID(env, handlerclass, "onReadReceipt", "(II)V");
    cache->onConnectionStatusMethodId = (*env)->GetMethodID(env, handlerclass, "onConnectionStatus", "(IZ)V");
    cache->onTypingChangeMethodId = (*env)->GetMethodID(env, handlerclass, "onTypingChange", "(IZ)V");
    cache->onAudioDataMethodId = (*env)->GetMethodID(env, handlerclass,
                                                     "onAudioData", "(I[B)V");
    cache->onVideoDataMethodId = (*env)->GetMethodID(env, handlerclass,
                                                     "onVideoData", "(I[BII)V");
    cache->onAvCallbackMethodId = (*env)->GetMethodID(env, handlerclass, "onAvCallback", "(ILim/tox/jtoxcore/ToxAvCallbackID;)V");

    return JNI_VERSION_1_6;
}

JNIEXPORT jlong JNICALL Java_im_tox_jtoxcore_JTox_tox_1new(JNIEnv *env, jobject jobj, jobject tox_options)
{
	tox_jni_globals_t *globals = malloc(sizeof(tox_jni_globals_t));
	JavaVM *jvm;
    Tox_Options tox_options_native;
	jclass clazz = (*env)->GetObjectClass(env, jobj);
	jfieldID id = (*env)->GetFieldID(env, clazz, "handler", "Lim/tox/jtoxcore/callbacks/CallbackHandler;");
	jobject handler = (*env)->GetObjectField(env, jobj, id);
	jobject handlerRef = (*env)->NewGlobalRef(env, handler);
	jobject jtoxRef = (*env)->NewGlobalRef(env, jobj);
	(*env)->GetJavaVM(env, &jvm);
    tox_options_native = tox_options_to_native(env, tox_options);
	globals->tox = tox_new(&tox_options_native);
	globals->jvm = jvm;
	globals->handler = handlerRef;
	globals->jtox = jtoxRef;
    globals->cache = cache;

	tox_callback_friend_action(globals->tox, callback_action, globals);

	tox_callback_connection_status(globals->tox, callback_connectionstatus, globals);

	tox_callback_friend_message(globals->tox, callback_friendmessage, globals);

	tox_callback_friend_request(globals->tox, callback_friendrequest, globals);

	tox_callback_name_change(globals->tox, callback_namechange, globals);

	tox_callback_read_receipt(globals->tox, callback_read_receipt, globals);

	tox_callback_status_message(globals->tox, callback_statusmessage, globals);

	tox_callback_user_status(globals->tox, callback_userstatus, globals);

	tox_callback_typing_change(globals->tox, callback_typingstatus, globals);

	tox_callback_file_control(globals->tox, callback_filecontrol, globals);

	tox_callback_file_send_request(globals->tox, callback_filesendrequest, globals);

	tox_callback_file_data(globals->tox, callback_filedata, globals);

	return ((jlong) ((intptr_t) globals));
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_tox_1bootstrap_1from_1address(JNIEnv *env, jobject obj,
		jlong messenger, jstring ip, jint port, jbyteArray address)
{
	const char *_ip = (*env)->GetStringUTFChars(env, ip, 0);
	jbyte *_address = (*env)->GetByteArrayElements(env, address, 0);

	uint16_t _port = (uint16_t) port;

	jint result = tox_bootstrap_from_address(((tox_jni_globals_t *) ((intptr_t) messenger))->tox, _ip, _port,
				  (uint8_t *) _address);

	(*env)->ReleaseStringUTFChars(env, ip, _ip);
	(*env)->ReleaseByteArrayElements(env, address, _address, JNI_ABORT);

	UNUSED(obj);
	return result;
}

JNIEXPORT void JNICALL Java_im_tox_jtoxcore_JTox_tox_1do(JNIEnv *env, jobject obj, jlong messenger)
{
	tox_do(((tox_jni_globals_t *) ((intptr_t) messenger))->tox);
	UNUSED(env);
	UNUSED(obj);
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_tox_1do_1interval(JNIEnv *env, jobject obj, jlong messenger)
{
	jint result = tox_do_interval(((tox_jni_globals_t *) ((intptr_t) messenger))->tox);
	UNUSED(env);
	UNUSED(obj);
	return result;
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_tox_1isconnected(JNIEnv *env, jobject obj, jlong messenger)
{
	UNUSED(env);
	UNUSED(obj);
	return tox_isconnected(((tox_jni_globals_t *) ((intptr_t) messenger))->tox);
}

JNIEXPORT void JNICALL Java_im_tox_jtoxcore_JTox_tox_1kill(JNIEnv *env, jobject jobj, jlong messenger)
{
	tox_jni_globals_t *globals = (tox_jni_globals_t *) ((intptr_t) messenger);
	tox_kill(globals->tox);
	(*env)->DeleteGlobalRef(env, globals->handler);
	(*env)->DeleteGlobalRef(env, globals->jtox);
	free(globals->cache);
	free(globals);
	UNUSED(jobj);
}

JNIEXPORT jbyteArray JNICALL Java_im_tox_jtoxcore_JTox_tox_1save(JNIEnv *env, jobject obj, jlong messenger)
{
	Tox *tox = ((tox_jni_globals_t *) ((intptr_t) messenger))->tox;
	uint32_t size = tox_size(tox);
	uint8_t *data = malloc(size);
	jbyteArray bytes = (*env)->NewByteArray(env, size);
	tox_save(tox, data);
	(*env)->SetByteArrayRegion(env, bytes, 0, size, (jbyte *) data);
	free(data);

	UNUSED(obj);
	return bytes;
}

JNIEXPORT jboolean JNICALL Java_im_tox_jtoxcore_JTox_tox_1load(JNIEnv *env, jobject obj, jlong messenger,
		jbyteArray bytes, jint length)
{
	jbyte *data = (*env)->GetByteArrayElements(env, bytes, 0);

	UNUSED(obj);
	int ret = tox_load(((tox_jni_globals_t *) ((intptr_t) messenger))->tox, (uint8_t *) data, length);

	if (ret == -1)
	    return JNI_FALSE;
	else
	    return JNI_TRUE;
}

/**
 * End maintenance section
 */

/**
 * Begin general section
 */

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_tox_1add_1friend(JNIEnv *env, jobject obj, jlong messenger,
		jbyteArray address, jbyteArray data, jint length)
{
	jbyte *_address = (*env)->GetByteArrayElements(env, address, 0);
	jbyte *_data = (*env)->GetByteArrayElements(env, data, 0);

	int ret = tox_add_friend(((tox_jni_globals_t *)((intptr_t)messenger))->tox, (uint8_t *) _address, (uint8_t *) _data,
							 length);

	(*env)->ReleaseByteArrayElements(env, address, _address, JNI_ABORT);
	(*env)->ReleaseByteArrayElements(env, data, _data, JNI_ABORT);

	UNUSED(obj);
	return ret;
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_tox_1add_1friend_1norequest(JNIEnv *env, jobject obj, jlong messenger,
		jbyteArray address)
{
	jbyte *_address = (*env)->GetByteArrayElements(env, address, 0);

	int ret = tox_add_friend_norequest(((tox_jni_globals_t *) ((intptr_t) messenger))->tox, (uint8_t *) _address);
	(*env)->ReleaseByteArrayElements(env, address, _address, JNI_ABORT);

	UNUSED(obj);
	return ret;
}

JNIEXPORT jstring JNICALL Java_im_tox_jtoxcore_JTox_tox_1get_1address(JNIEnv *env, jobject obj, jlong messenger)
{
	jstring result;
	uint8_t addr[TOX_FRIEND_ADDRESS_SIZE];
	char id[ADDR_SIZE_HEX] = { 0 };
	tox_get_address(((tox_jni_globals_t *)((intptr_t) messenger))->tox, addr);
	addr_to_hex(addr, id);

	UNUSED(obj);
	result = (*env)->NewStringUTF(env, id);
	return result;
}

JNIEXPORT jstring JNICALL Java_im_tox_jtoxcore_JTox_tox_1get_1client_1id(JNIEnv *env, jobject obj, jlong messenger,
		jint friendnumber)
{
	uint8_t address[TOX_FRIEND_ADDRESS_SIZE];
	jstring result;
	UNUSED(obj);

	if (tox_get_client_id(((tox_jni_globals_t *)((intptr_t) messenger))->tox, friendnumber, address) != 0) {
		return 0;
	} else {
		char _address[ADDR_SIZE_HEX] = { 0 };
		addr_to_hex(address, _address);
		result = (*env)->NewStringUTF(env, _address);
		return result;
	}
}

JNIEXPORT jboolean JNICALL Java_im_tox_jtoxcore_JTox_tox_1del_1friend(JNIEnv *env, jobject obj, jlong messenger,
		jint friendnumber)
{
	UNUSED(env);
	UNUSED(obj);
	return tox_del_friend(((tox_jni_globals_t *) ((intptr_t) messenger))->tox, friendnumber) == 0 ? 0 : 1;
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_tox_1send_1message(JNIEnv *env, jobject obj, jlong messenger,
		jint friendnumber, jbyteArray message, jint length)
{
	jbyte *_message = (*env)->GetByteArrayElements(env, message, 0);

	uint32_t mess_id = tox_send_message(((tox_jni_globals_t *) ((intptr_t) messenger))->tox, friendnumber,
										(uint8_t *) _message,
										length);
	(*env)->ReleaseByteArrayElements(env, message, _message, JNI_ABORT);

	UNUSED(obj);
	return mess_id;
}

JNIEXPORT jboolean JNICALL Java_im_tox_jtoxcore_JTox_tox_1send_1action(JNIEnv *env, jobject obj, jlong messenger,
		jint friendnumber, jbyteArray action, jint length)
{
	jbyte *_action = (*env)->GetByteArrayElements(env, action, 0);

	jboolean ret = tox_send_action(((tox_jni_globals_t *) ((intptr_t) messenger))->tox, friendnumber, (uint8_t *) _action,
								   length);
	(*env)->ReleaseByteArrayElements(env, action, _action, JNI_ABORT);

	UNUSED(obj);
	return ret;
}

JNIEXPORT jboolean JNICALL Java_im_tox_jtoxcore_JTox_tox_1set_1name(JNIEnv *env, jobject obj, jlong messenger,
		jbyteArray newname, jint length)
{
	jbyte *_newname = (*env)->GetByteArrayElements(env, newname, 0);

	jboolean ret =
		tox_set_name(((tox_jni_globals_t *) ((intptr_t) messenger))->tox, (uint8_t *) _newname, length) == 0 ?
		JNI_FALSE : JNI_TRUE;
	(*env)->ReleaseByteArrayElements(env, newname, _newname, JNI_ABORT);

	UNUSED(obj);
	return ret;
}

JNIEXPORT jstring JNICALL Java_im_tox_jtoxcore_JTox_tox_1get_1self_1name(JNIEnv *env, jobject obj, jlong messenger)
{
	jstring _name;
	uint8_t *name = malloc(TOX_MAX_NAME_LENGTH);
	uint16_t length = tox_get_self_name(((tox_jni_globals_t *) ((intptr_t) messenger))->tox, name);

	if (length == 0) {
		free(name);
		return 0;
	}

	_name = (*env)->NewStringUTF(env, (char *) name);
	free(name);

	UNUSED(obj);
	return _name;
}

JNIEXPORT jboolean JNICALL Java_im_tox_jtoxcore_JTox_tox_1set_1status_1message(JNIEnv *env, jobject obj,
		jlong messenger, jbyteArray newstatus, jint length)
{
	jbyte *_newstatus = (*env)->GetByteArrayElements(env, newstatus, 0);
	jboolean ret =
		tox_set_status_message(((tox_jni_globals_t *) ((intptr_t) messenger))->tox, (uint8_t *) _newstatus, length) == 0 ?
		JNI_FALSE :
		JNI_TRUE;
	(*env)->ReleaseByteArrayElements(env, newstatus, _newstatus, JNI_ABORT);

	UNUSED(obj);
	return ret;
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_tox_1get_1friend_1connection_1status(JNIEnv *env, jobject obj,
		jlong messenger, jint friendnumber)
{
	uint32_t ret = tox_get_friend_connection_status(((tox_jni_globals_t *)((intptr_t)messenger))->tox, friendnumber);

	UNUSED(env);
	UNUSED(obj);
	return ret;
}

JNIEXPORT jboolean JNICALL Java_im_tox_jtoxcore_JTox_tox_1get_1friend_1exists(JNIEnv *env, jobject obj, jlong messenger,
		jint friendnumber)
{
	uint8_t ret = tox_friend_exists(((tox_jni_globals_t *)((intptr_t)messenger))->tox, friendnumber);

	UNUSED(env);
	UNUSED(obj);
	return ret;
}

JNIEXPORT jbyteArray JNICALL Java_im_tox_jtoxcore_JTox_tox_1get_1name(JNIEnv *env, jobject obj, jlong messenger,
		jint friendnumber)
{
	jbyte *name = malloc(TOX_MAX_NAME_LENGTH);
	int ret = tox_get_name(((tox_jni_globals_t *) ((intptr_t) messenger))->tox, friendnumber, (uint8_t *) name);

	UNUSED(obj);

	if (ret == -1) {
		free(name);
		return 0;
	} else {
		jbyteArray _name = (*env)->NewByteArray(env, ret);
		(*env)->SetByteArrayRegion(env, _name, 0, ret, name);
		free(name);
		return _name;
	}
}

/*
JNIEXPORT jobjectArray JNICALL Java_im_tox_jtoxcore_JTox_tox_lgroup_lget_lnames(JNIEnv *env,
        jobject obj, jlong messenger, jint groupnumber)
{
    int num_peers = tox_group_number_peers(((tox_jni_globals_t *) ((intptr_t) messenger))->tox, groupnumber);
    jbyte **names;
    names = (jbyte **) malloc(num_peers*sizeof(jbyte*));
    int i;
    for (i = 0; i < num_peers; i++) {
        names[i] = (jbyte*) malloc(TOX_MAX_NAME_LENGTH*sizeof(jbyte));
    }
    jshort *lengths = malloc(num_peers*sizeof(jshort));
    jshort length;
    int ret = tox_group_get_names(((tox_jni_globals_t *) ((intptr_t) messenger))->tox, groupnumber, (uint8_t [][TOX_MAX_NAME_LENGTH]) names, lengths, length);
    if (ret == -1) {
        return NULL;
    } else {
        // The 2D array to return
        jbyte** primitive2DArray = names;

        // Get the array class
        jclass byteArrayClass = (*env)->FindClass(env, "[B");

        // Check if we properly got the array class
        if (byteArrayClass == NULL) {
            return NULL;
        }

        // Create the returnable 2D array
        jobjectArray returnable2DArray = (*env)->NewObjectArray(env, (jsize) num_peers, byteArrayClass, NULL);

        // Go through the first dimension and add the second dimension arrays
        for (i = 0; i < num_peers; i++) {
            jbyteArray byteArray = (*env)->NewByteArray(env, TOX_MAX_NAME_LENGTH);
            (*env)->SetByteArrayRegion(env, byteArray, (jsize) 0, (jsize) TOX_MAX_NAME_LENGTH, (jbyte*) primitive2DArray[i]);
            (*env)->SetObjectArrayElement(env, returnable2DArray, (jsize) i, byteArray);
            (*env)->DeleteLocalRef(env, byteArray);
        }
        // Return a Java consumable 2D long array
        return returnable2DArray;
    }
}
*/

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_tox_1get_1nospam(JNIEnv *env, jobject obj, jlong messenger)
{
	int result = tox_get_nospam(((tox_jni_globals_t *) ((intptr_t) messenger))->tox);
	UNUSED(obj);
	UNUSED(env);
	return result;
}
JNIEXPORT void JNICALL Java_im_tox_jtoxcore_JTox_tox_1set_1nospam(JNIEnv *env, jobject obj, jlong messenger, jint nospam)
{
	tox_set_nospam(((tox_jni_globals_t *) ((intptr_t) messenger))->tox, nospam);
	UNUSED(obj);
	UNUSED(env);
}
// FILE SENDING BEGINS
JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_tox_1new_1file_1sender(JNIEnv *env, jobject obj, jlong messenger,
		jint friendnumber, jlong filesize, jbyteArray filename, jint length)
{
	jbyte *_filename = (*env)->GetByteArrayElements(env, filename, 0);
	int result = tox_new_file_sender(((tox_jni_globals_t *) ((intptr_t) messenger))->tox, friendnumber, filesize,
									 (uint8_t *) _filename, length);
	(*env)->ReleaseByteArrayElements(env, filename, _filename, JNI_ABORT);
	UNUSED(obj);
	return result;
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_tox_1file_1send_1control(JNIEnv *env, jobject obj, jlong messenger,
		jint friendnumber, jint send_receive, jint filenumber, jint message_id, jbyteArray data, jint length)
{
	jbyte *_data = (*env)->GetByteArrayElements(env, data, 0);
	int result = tox_file_send_control(((tox_jni_globals_t *) ((intptr_t) messenger))->tox, friendnumber, send_receive,
									   filenumber, message_id, (uint8_t *) _data, length);
	(*env)->ReleaseByteArrayElements(env, data, _data, JNI_ABORT);
	UNUSED(obj);
	return result;
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_tox_1file_1send_1data(JNIEnv *env, jobject obj, jlong messenger,
		jint friendnumber, jint filenumber, jbyteArray data, jint length)
{
	jbyte *_data = (*env)->GetByteArrayElements(env, data, 0);
	int result = tox_file_send_data(((tox_jni_globals_t *) ((intptr_t) messenger))->tox, friendnumber, filenumber,
									(uint8_t *) _data, length);
	(*env)->ReleaseByteArrayElements(env, data, _data, JNI_ABORT);
	UNUSED(obj);
	return result;
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_tox_1file_1data_1size(JNIEnv *env, jobject obj, jlong messenger,
		jint friendnumber)
{
	int result = tox_file_data_size(((tox_jni_globals_t *) ((intptr_t) messenger))->tox, friendnumber);
	UNUSED(obj);
	UNUSED(env);
	return result;
}

JNIEXPORT jlong JNICALL Java_im_tox_jtoxcore_JTox_tox_1file_1data_1remaining(JNIEnv *env, jobject obj, jlong messenger,
		jint friendnumber, jint filenumber, jint send_receive)
{
	long result = tox_file_data_remaining(((tox_jni_globals_t *) ((intptr_t) messenger))->tox, friendnumber, filenumber,
										  send_receive);
	UNUSED(obj);
	UNUSED(env);
	return result;
}
// FILE SENDING ENDS

JNIEXPORT jboolean JNICALL Java_im_tox_jtoxcore_JTox_tox_1set_1user_1status(JNIEnv *env, jobject obj, jlong messenger,
		jint userstatus)
{
	UNUSED(env);
	UNUSED(obj);

	return tox_set_user_status(((tox_jni_globals_t *) ((intptr_t) messenger))->tox, userstatus) == 0 ?
		   JNI_FALSE : JNI_TRUE;
}

JNIEXPORT jbyteArray JNICALL Java_im_tox_jtoxcore_JTox_tox_1get_1status_1message(JNIEnv *env, jobject obj,
		jlong messenger, jint friendnumber)
{
	Tox *tox = ((tox_jni_globals_t *) ((intptr_t) messenger))->tox;
	int size = tox_get_status_message_size(tox, friendnumber);
	jbyte *statusmessage = malloc(size);
	int ret = tox_get_status_message(tox, friendnumber, (uint8_t *) statusmessage, size);

	UNUSED(obj);

	if (ret == -1) {
		free(statusmessage);
		return 0;
	} else {
		jbyteArray _statusmessage = (*env)->NewByteArray(env, ret);
		(*env)->SetByteArrayRegion(env, _statusmessage, 0, ret, statusmessage);
		free(statusmessage);
		return _statusmessage;
	}
}

JNIEXPORT jbyteArray JNICALL Java_im_tox_jtoxcore_JTox_tox_1getselfstatusmessage(JNIEnv *env, jobject obj,
		jlong messenger)
{
	Tox *tox = ((tox_jni_globals_t *) ((intptr_t) messenger))->tox;
	jbyte *status = malloc(TOX_MAX_STATUSMESSAGE_LENGTH);
	int length = tox_get_self_status_message(tox, (uint8_t *) status,
				 TOX_MAX_STATUSMESSAGE_LENGTH);

	UNUSED(obj);

	if (length == -1) {
		free(status);
		return 0;
	} else {
		jbyteArray _status = (*env)->NewByteArray(env, length);
		(*env)->SetByteArrayRegion(env, _status, 0, length, status);
		free(status);
		return _status;
	}
}

JNIEXPORT jobject JNICALL Java_im_tox_jtoxcore_JTox_tox_1get_1user_1status(JNIEnv *env, jobject obj, jlong messenger,
		jint friendnumber)
{
	Tox *tox = ((tox_jni_globals_t *) ((intptr_t) messenger))->tox;
	char *status;
	jclass us_enum;
	jfieldID fieldID;

	switch (tox_get_user_status(tox, friendnumber)) {
		case TOX_USERSTATUS_NONE:
			status = "TOX_USERSTATUS_NONE";
			break;

		case TOX_USERSTATUS_AWAY:
			status = "TOX_USERSTATUS_AWAY";
			break;

		case TOX_USERSTATUS_BUSY:
			status = "TOX_USERSTATUS_BUSY";
			break;

		default:
			status = "TOX_USERSTATUS_INVALID";
			break;
	}

	us_enum = (*env)->FindClass(env, "im/tox/jtoxcore/ToxUserStatus");
	fieldID = (*env)->GetStaticFieldID(env, us_enum, status, "Lim/tox/jtoxcore/ToxUserStatus;");

	UNUSED(obj);
	return (*env)->GetStaticObjectField(env, us_enum, fieldID);
}

JNIEXPORT jobject JNICALL Java_im_tox_jtoxcore_JTox_tox_1get_1self_1user_1status(JNIEnv *env, jobject obj,
		jlong messenger)
{
	Tox *tox = ((tox_jni_globals_t *) ((intptr_t) messenger))->tox;
	char *status;
	jclass us_enum;
	jfieldID fieldID;

	switch (tox_get_self_user_status(tox)) {
		case TOX_USERSTATUS_NONE:
			status = "TOX_USERSTATUS_NONE";
			break;

		case TOX_USERSTATUS_AWAY:
			status = "TOX_USERSTATUS_AWAY";
			break;

		case TOX_USERSTATUS_BUSY:
			status = "TOX_USERSTATUS_BUSY";
			break;

		default:
			status = "TOX_USERSTATUS_INVALID";
			break;
	}

	us_enum = (*env)->FindClass(env, "im/tox/jtoxcore/ToxUserStatus");
	fieldID = (*env)->GetStaticFieldID(env, us_enum, status, "Lim/tox/jtoxcore/ToxUserStatus;");

	UNUSED(obj);
	return (*env)->GetStaticObjectField(env, us_enum, fieldID);
}

JNIEXPORT jintArray JNICALL Java_im_tox_jtoxcore_JTox_tox_1get_1friendlist(JNIEnv *env, jobject obj, jlong messenger)
{
	Tox *tox = ((tox_jni_globals_t *) ((intptr_t) messenger))->tox;
	uint32_t length = tox_count_friendlist(tox);
	int *list = malloc(length);
	uint32_t actual_length = tox_get_friendlist(tox, list, length);
	jintArray arr = (*env)->NewIntArray(env, actual_length);
	(*env)->SetIntArrayRegion(env, arr, 0, actual_length, (jint *) list);
	free(list);

	UNUSED(obj);
	return arr;
}

JNIEXPORT jboolean JNICALL Java_im_tox_jtoxcore_JTox_tox_1set_1user_1is_1typing
(JNIEnv *env, jobject obj, jlong messenger, jint friendnumber, jboolean typing)
{
	Tox *tox = ((tox_jni_globals_t *)((intptr_t) messenger))->tox;
	uint8_t is_typing;

	if (typing == JNI_TRUE) {
		is_typing = 1;
	} else {
		is_typing = 0;
	}

	UNUSED(env);
	UNUSED(obj);
	return tox_set_user_is_typing(tox, friendnumber, is_typing) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_im_tox_jtoxcore_JTox_tox_1get_1is_1typing
(JNIEnv *env, jobject obj, jlong messenger, jint friendnumber)
{
	Tox *tox = ((tox_jni_globals_t *) ((intptr_t) messenger))->tox;

	uint8_t is_typing = tox_get_is_typing(tox, friendnumber);

	UNUSED(env);
	UNUSED(obj);
	return is_typing == 1 ? JNI_TRUE : JNI_FALSE;
}
////////////////////////////// AUDIO / VIDEO////////////////////////////////////

JNIEXPORT jlong JNICALL Java_im_tox_jtoxcore_JTox_toxav_1new
(JNIEnv *env, jobject obj, jlong messenger, jint max_calls)
{
	tox_av_jni_globals_t *globals = malloc(sizeof(tox_av_jni_globals_t));
	Tox *tox = ((tox_jni_globals_t *) ((intptr_t) messenger))->tox;
	JavaVM *jvm;
	jclass clazz = (*env)->GetObjectClass(env, obj);
	jfieldID id = (*env)->GetFieldID(env, clazz, "handler", "Lim/tox/jtoxcore/callbacks/CallbackHandler;");
	jobject handler = (*env)->GetObjectField(env, obj, id);
	jobject handlerRef = (*env)->NewGlobalRef(env, handler);
	jobject jtoxRef = (*env)->NewGlobalRef(env, obj);
	(*env)->GetJavaVM(env, &jvm);
	globals->toxav = toxav_new(tox, (int32_t) max_calls);
	globals->jvm = jvm;
	globals->handler = handlerRef;
	globals->jtox = jtoxRef;
    globals->cache = cache;

	toxav_register_callstate_callback(globals->toxav, avcallback_invite, av_OnInvite, globals);
	toxav_register_callstate_callback(globals->toxav, avcallback_start, av_OnStart, globals);
	toxav_register_callstate_callback(globals->toxav, avcallback_cancel, av_OnCancel, globals);
	toxav_register_callstate_callback(globals->toxav, avcallback_reject, av_OnReject, globals);
	toxav_register_callstate_callback(globals->toxav, avcallback_end, av_OnEnd, globals);
	toxav_register_callstate_callback(globals->toxav, avcallback_ringing, av_OnRinging, globals);
	toxav_register_callstate_callback(globals->toxav, avcallback_starting, av_OnStarting, globals);
	toxav_register_callstate_callback(globals->toxav, avcallback_ending, av_OnEnding, globals);
	toxav_register_callstate_callback(globals->toxav, avcallback_requesttimeout, av_OnRequestTimeout, globals);
	toxav_register_callstate_callback(globals->toxav, avcallback_peertimeout, av_OnPeerTimeout, globals);
	toxav_register_callstate_callback(globals->toxav, avcallback_mediachange, av_OnMediaChange, globals);
	toxav_register_audio_recv_callback(globals->toxav, avcallback_audio, globals);
	toxav_register_video_recv_callback(globals->toxav, avcallback_video, globals);

	return ((jlong) ((intptr_t) globals));
}

JNIEXPORT void JNICALL Java_im_tox_jtoxcore_JTox_toxav_1kill
(JNIEnv *env, jobject obj, jlong messenger)
{
	ToxAv *tox_av = ((tox_av_jni_globals_t *) ((intptr_t) messenger))->toxav;
	tox_av_jni_globals_t *globals = (tox_av_jni_globals_t *) ((intptr_t) messenger);
	toxav_kill(tox_av);
	(*env)->DeleteGlobalRef(env, globals->handler);
	(*env)->DeleteGlobalRef(env, globals->jtox);
	free(globals->cache);
	free(globals);
	UNUSED(obj);
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_toxav_1call
(JNIEnv *env, jobject obj, jlong messenger, jint friend_id, jobject codec_settings, jint ringing_seconds)
{
	ToxAvCSettings codec_settings_native;
	int32_t id;
	jint res;

	ToxAv *tox_av = ((tox_av_jni_globals_t *) ((intptr_t) messenger))->toxav;
	codec_settings_native = codec_settings_to_native(env, codec_settings);
	res = toxav_call(tox_av, &id, friend_id, &codec_settings_native, ringing_seconds);
	UNUSED(obj);
	return res;
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_toxav_1hangup
(JNIEnv *env, jobject obj, jlong messenger, jint call_index)
{
	ToxAv *tox_av = ((tox_av_jni_globals_t *) ((intptr_t) messenger))->toxav;
	jint res = toxav_hangup(tox_av, (int32_t) call_index);
	UNUSED(obj);
	UNUSED(env);
	return res;
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_toxav_1answer
(JNIEnv *env, jobject obj, jlong messenger, jint call_index, jobject codec_settings)
{
	ToxAvCSettings codec_settings_native;
	jint res;

	ToxAv *tox_av = ((tox_av_jni_globals_t *) ((intptr_t) messenger))->toxav;
	codec_settings_native = codec_settings_to_native(env, codec_settings);
	res = toxav_answer(tox_av, (int32_t) call_index, &codec_settings_native);
	UNUSED(obj);
	return res;
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_toxav_1reject
(JNIEnv *env, jobject obj, jlong messenger, jint call_index, jstring reason)
{
	ToxAv *tox_av = ((tox_av_jni_globals_t *) ((intptr_t) messenger))->toxav;
	const char *reason_native = (*env)->GetStringUTFChars(env, reason, 0);
	jint res = toxav_reject(tox_av, (int32_t) call_index, reason_native);
	UNUSED(obj);
	return res;
}


JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_toxav_1cancel
(JNIEnv *env, jobject obj, jlong messenger, jint call_index, jint peer_id, jstring reason)
{
	ToxAv *tox_av = ((tox_av_jni_globals_t *) ((intptr_t) messenger))->toxav;
	const char *reason_native = (*env)->GetStringUTFChars(env, reason, 0);
	jint res = toxav_cancel(tox_av, (int32_t) call_index, (int) peer_id, reason_native);
	UNUSED(obj);
	return res;
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_toxav_1change_1settings
(JNIEnv *env, jobject obj, jlong messenger, jint call_index, jobject codec_settings)
{
	ToxAvCSettings codec_settings_native;
	jint res;

	ToxAv *tox_av = ((tox_av_jni_globals_t *) ((intptr_t) messenger))->toxav;
	codec_settings_native = codec_settings_to_native(env, codec_settings);
	res = toxav_change_settings(tox_av, (int32_t) call_index, &codec_settings_native);
	UNUSED(obj);
	return res;
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_toxav_1stop_1call
(JNIEnv *env, jobject obj, jlong messenger, jint call_index)
{
	ToxAv *tox_av = ((tox_av_jni_globals_t *) ((intptr_t) messenger))->toxav;
	jint res = toxav_stop_call(tox_av, (int32_t) call_index);
	UNUSED(obj);
	UNUSED(env);
	return res;
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_toxav_1prepare_1transmission
(JNIEnv *env, jobject obj, jlong messenger, jint call_index,
 jint jbuf_size, jint VAD_threshold, jint support_video)
{
	ToxAv *tox_av = ((tox_av_jni_globals_t *) ((intptr_t) messenger))->toxav;
	jint res = toxav_prepare_transmission(tox_av, (int32_t) call_index, (uint32_t) jbuf_size, (uint32_t) VAD_threshold, (int) support_video);
	UNUSED(obj);
	UNUSED(env);
	return res;
}


JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_toxav_1kill_1transmission
(JNIEnv *env, jobject obj, jlong messenger, jint call_index)
{
	ToxAv *tox_av = ((tox_av_jni_globals_t *) ((intptr_t) messenger))->toxav;
	jint res = toxav_kill_transmission(tox_av, (int32_t) call_index);
	UNUSED(obj);
	UNUSED(env);
	return res;
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_toxav_1send_1video
(JNIEnv *env, jobject obj, jlong messenger, jint call_index,
 jbyteArray frame, jint frame_size)
{
	ToxAv *tox_av = ((tox_av_jni_globals_t *) ((intptr_t) messenger))->toxav;
	jbyte *_frame = (*env)->GetByteArrayElements(env, frame, 0);
	jint res = toxav_send_video(tox_av, (int32_t) call_index, (uint8_t *) _frame, frame_size);
	(*env)->ReleaseByteArrayElements(env, frame, _frame, JNI_ABORT);
	UNUSED(obj);
	return res;
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_toxav_1send_1audio
(JNIEnv *env, jobject obj, jlong messenger, jint call_index,
 jbyteArray frame, jint frame_size)
{
	ToxAv *tox_av = ((tox_av_jni_globals_t *) ((intptr_t) messenger))->toxav;
	jbyte *_frame = (*env)->GetByteArrayElements(env, frame, 0);
	jint res = toxav_send_audio(tox_av, (int32_t) call_index, (uint8_t *) _frame, frame_size);
	(*env)->ReleaseByteArrayElements(env, frame, _frame, JNI_ABORT);
	UNUSED(obj);
	return res;
}


JNIEXPORT jbyteArray JNICALL Java_im_tox_jtoxcore_JTox_toxav_1prepare_1video_1frame
(JNIEnv *env, jobject obj, jlong messenger, jint call_index, jint dest_max, jbyteArray data, jint width, jint height)
{
	jbyteArray output;
	vpx_image_t img;
	ToxAv *tox_av = ((tox_av_jni_globals_t *) ((intptr_t) messenger))->toxav;
	int stride = ALIGN(width, 16);
	int y_size = stride * height;
	int c_stride = ALIGN(stride / 2, 16);
	int c_size = c_stride * height / 2;
	//int size = y_size + c_size*2;
	int cr_offset = y_size;
	int cb_offset = y_size + c_size;
	uint8_t y;
	uint8_t v;
	uint8_t u;
	jbyte *_data = (*env)->GetByteArrayElements(env, data, 0);
	vpx_img_alloc(&img, VPX_IMG_FMT_YV12, width, height, 1);
	unsigned long int y_coord, x_coord;

	for (y_coord = 0; y_coord < img.d_h; ++y_coord) {
		for (x_coord = 0; x_coord < img.d_w; ++x_coord) {
			y = *(_data + (y_coord * stride) + x_coord);
			img.planes[0][y * (img.stride[0] + x_coord)] = y;

			if (x_coord == (x_coord / 2) * 2) {
				v = *(_data + cr_offset + (y_coord * c_stride) + x_coord / 2);
				img.planes[1][y * (img.stride[1] + x_coord / 2)] = v;
				u = *(_data + cb_offset + (y_coord * c_stride) + x_coord / 2);
				img.planes[2][y * (img.stride[2] + x_coord / 2)] = u;
			}
		}
	}

	jbyte *dest = malloc(sizeof(jbyte) * dest_max);
	jint res = toxav_prepare_video_frame(tox_av, (int32_t) call_index,
										 (uint8_t *) dest, dest_max, &img);
	(*env)->ReleaseByteArrayElements(env, data, _data, JNI_ABORT);
	output = (*env)->NewByteArray(env, res);
	(*env)->SetByteArrayRegion(env, output, 0, res, dest);
	free(dest);

	UNUSED(obj);
	return output;
}

JNIEXPORT jbyteArray JNICALL Java_im_tox_jtoxcore_JTox_toxav_1prepare_1audio_1frame
(JNIEnv *env, jobject obj, jlong messenger, jint call_index, jint dest_max, jintArray frame, jint frame_size)
{
	jbyteArray output;
	jbyte *dest = malloc(sizeof(jbyte) * dest_max);
	ToxAv *tox_av = ((tox_av_jni_globals_t *) ((intptr_t) messenger))->toxav;
	jint *_frame = (*env)->GetIntArrayElements(env, frame, 0);
	jint res = toxav_prepare_audio_frame(tox_av, (int32_t) call_index,
										 (uint8_t *) dest, dest_max, (int16_t *) _frame, frame_size);
	output = (*env)->NewByteArray(env, res);
	(*env)->SetByteArrayRegion(env, output, 0, res, dest);
	free(dest);
	(*env)->ReleaseIntArrayElements(env, frame, _frame, JNI_ABORT);

	UNUSED(obj);
	return output;
}

JNIEXPORT jobject JNICALL Java_im_tox_jtoxcore_JTox_toxav_1get_1peer_1csettings
(JNIEnv *env, jobject obj, jlong messenger, jint call_index, jint peer)
{
	ToxAvCSettings _dest;
	ToxAv *tox_av;
	jobject java_codec_settings;

	tox_av = ((tox_av_jni_globals_t *) ((intptr_t) messenger))->toxav;
	toxav_get_peer_csettings(tox_av, (int32_t) call_index, peer, &_dest);
	java_codec_settings = codec_settings_to_java(env, _dest);
	UNUSED(obj);
	return java_codec_settings;
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_toxav_1get_1peer_1id
(JNIEnv *env, jobject obj, jlong messenger, jint call_index, jint peer)
{
	ToxAv *tox_av = ((tox_av_jni_globals_t *) ((intptr_t) messenger))->toxav;
	jint res = toxav_get_peer_id(tox_av, (int32_t) call_index, peer);
	UNUSED(obj);
	UNUSED(env);
	return res;
}

JNIEXPORT jobject JNICALL Java_im_tox_jtoxcore_JTox_toxav_1get_1call_1state
(JNIEnv *env, jobject obj, jlong messenger, jint call_index)
{
	jfieldID enum_field_id;
	jobject call_state;
	jclass enum_class;
	ToxAv *tox_av = ((tox_av_jni_globals_t *) ((intptr_t) messenger))->toxav;
	ToxAvCallState res = toxav_get_call_state(tox_av, (int32_t) call_index);
	enum_class = (*env)->FindClass(env, "im/tox/jtoxcore/ToxAvCallState");

	switch (res) {
		case av_CallNonExistant:
			enum_field_id = (*env)->GetStaticFieldID(env, enum_class, "CALL_NONEXISTANT", "Lim/tox/jtoxcore/ToxAvCallState;");
			break;
		case av_CallInviting:
			enum_field_id = (*env)->GetStaticFieldID(env, enum_class, "CALL_INVITING", "Lim/tox/jtoxcore/ToxAvCallState;");
			break;
		case av_CallStarting:
			enum_field_id = (*env)->GetStaticFieldID(env, enum_class, "CALL_STARTING", "Lim/tox/jtoxcore/ToxAvCallState;");
			break;
		case av_CallActive:
			enum_field_id = (*env)->GetStaticFieldID(env, enum_class, "CALL_ACTIVE", "Lim/tox/jtoxcore/ToxAvCallState;");
			break;
		case av_CallHold:
			enum_field_id = (*env)->GetStaticFieldID(env, enum_class, "CALL_HOLD", "Lim/tox/jtoxcore/ToxAvCallState;");
			break;
		case av_CallHanged_up:
			enum_field_id = (*env)->GetStaticFieldID(env, enum_class, "CALL_HANGED_UP", "Lim/tox/jtoxcore/ToxAvCallState;");
			break;
	}

	call_state = (*env)->GetStaticObjectField(env, enum_class, enum_field_id);
	UNUSED(obj);
	return call_state;
}

JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_toxav_1capability_1supported
(JNIEnv *env, jobject obj, jlong messenger, jint call_index, jobject capabilities)
{
	jclass enum_class;
	jmethodID get_name_method;
	jstring enum_name;
	const char *enum_name_native;
	ToxAvCapabilities capabilities_native;
	ToxAv *tox_av = ((tox_av_jni_globals_t *) ((intptr_t) messenger))->toxav;
	enum_class = (*env)->FindClass(env, "im/tox/jtoxcore/ToxAvCapabilities");
	get_name_method = (*env)->GetMethodID(env, enum_class, "name", "()Ljava/lang/String;");
	enum_name = (jstring)(*env)->CallObjectMethod(env, capabilities, get_name_method);
	enum_name_native = (*env)->GetStringUTFChars(env, enum_name, 0);

	if (strcmp(enum_name_native, "AUDIO_ENCODING") == 0) {
		capabilities_native = AudioEncoding;
	} else if (strcmp(enum_name_native, "AUDIO_DECODING") == 0) {
		capabilities_native = AudioDecoding;
	} else if (strcmp(enum_name_native, "VIDEO_ENCODING") == 0) {
		capabilities_native = VideoEncoding;
	} else if (strcmp(enum_name_native, "VIDEO_DECODING") == 0) {
		capabilities_native = VideoDecoding;
	}

	jint res = toxav_capability_supported(tox_av, (int32_t) call_index, capabilities_native);
	UNUSED(obj);
	return res;
}

/*
 * Class:     im_tox_jtoxcore_JTox
 * Method:    toxav_get_tox
 * Signature: (J)J
 *
JNIEXPORT jlong JNICALL Java_im_tox_jtoxcore_JTox_toxav_1get_1tox
  (JNIEnv *, jobject, jlong);

 *
 * Class:     im_tox_jtoxcore_JTox
 * Method:    toxav_has_activity
 * Signature: (JI[IIF)I
 *
JNIEXPORT jint JNICALL Java_im_tox_jtoxcore_JTox_toxav_1has_1activity
  (JNIEnv *env, jobject obj, jlong messenger, jint, jintArray, jint, jfloat);
**
 * End general section
 */

/**
 * Begin Callback Section
 */
static void callback_filecontrol(Tox *tox, int32_t friendnumber, uint8_t receive_send, uint8_t filenumber,
								 uint8_t control_type, uint8_t *data, uint16_t length, void *rptr)
{
	tox_jni_globals_t *ptr = (tox_jni_globals_t *) rptr;
	JNIEnv *env;
	jbyteArray _data;
	jclass control_enum;
	char *enum_name;
	jfieldID fieldID;
	jobject enum_val;

	ATTACH_THREAD(ptr, env);

	control_enum = (*env)->FindClass(env, "im/tox/jtoxcore/ToxFileControl");

	switch (control_type) {
		case TOX_FILECONTROL_ACCEPT:
			enum_name = "TOX_FILECONTROL_ACCEPT";
			break;

		case TOX_FILECONTROL_PAUSE:
			enum_name = "TOX_FILECONTROL_PAUSE";
			break;

		case TOX_FILECONTROL_KILL:
			enum_name = "TOX_FILECONTROL_KILL";
			break;

		case TOX_FILECONTROL_FINISHED:
			enum_name = "TOX_FILECONTROL_FINISHED";
			break;

		default:
			enum_name = "TOX_FILECONTROL_RESUME_BROKEN";
			break;
	}

	fieldID = (*env)->GetStaticFieldID(env, control_enum, enum_name, "Lim/tox/jtoxcore/ToxFileControl;");
	enum_val = (*env)->GetStaticObjectField(env, control_enum, fieldID);

	_data = (*env)->NewByteArray(env, length);
	(*env)->SetByteArrayRegion(env, _data, 0, length, (jbyte *) data);

    (*env)->CallVoidMethod(env, ptr->handler, ptr->cache->onFileControlMethodId, friendnumber, receive_send, filenumber, enum_val, _data);
	UNUSED(tox);
}

static void callback_filedata(Tox *tox, int32_t friendnumber, uint8_t filenumber, uint8_t *data, uint16_t length,
							  void *rptr)
{
	tox_jni_globals_t *ptr = (tox_jni_globals_t *) rptr;
	JNIEnv *env;
	jbyteArray _data;

    ATTACH_THREAD(ptr, env);

	_data = (*env)->NewByteArray(env, length);
	(*env)->SetByteArrayRegion(env, _data, 0, length, (jbyte *) data);

    (*env)->CallVoidMethod(env, ptr->handler, ptr->cache->onFileDataMethodId, friendnumber, filenumber, _data);

    (*env)->DeleteLocalRef(env, _data);

	UNUSED(tox);
}

static void callback_filesendrequest(Tox *tox, int32_t friendnumber, uint8_t filenumber, uint64_t filesize,
									 uint8_t *filename, uint16_t length, void *rptr)
{
	tox_jni_globals_t *ptr = (tox_jni_globals_t *) rptr;
	JNIEnv *env;
	jbyteArray _filename;

	ATTACH_THREAD(ptr, env);

	_filename = (*env)->NewByteArray(env, length);
	(*env)->SetByteArrayRegion(env, _filename, 0, length, (jbyte *) filename);

    (*env)->CallVoidMethod(env, ptr->handler, ptr->cache->onFileSendRequestMethodId, friendnumber, filenumber, filesize, _filename);
	UNUSED(tox);
}

static void callback_friendrequest(Tox *tox, uint8_t *pubkey, uint8_t *message, uint16_t length, void *rptr)
{
	tox_jni_globals_t *ptr = (tox_jni_globals_t *) rptr;
	JNIEnv *env;
	char buf[ADDR_SIZE_HEX] = { 0 };
	jstring _pubkey;
	jbyteArray _message;

	ATTACH_THREAD(ptr, env);	

	addr_to_hex(pubkey, buf);
	_pubkey = (*env)->NewStringUTF(env, buf);
	_message = (*env)->NewByteArray(env, length);
	(*env)->SetByteArrayRegion(env, _message, 0, length, (jbyte *) message);

    (*env)->CallVoidMethod(env, ptr->handler, ptr->cache->onFriendRequestMethodId, _pubkey, _message);
	UNUSED(tox);
}

static void callback_friendmessage(Tox *tox, int friendnumber, uint8_t *message, uint16_t length, void *rptr)
{
	tox_jni_globals_t *ptr = (tox_jni_globals_t *) rptr;
    JNIEnv *env;
	jbyteArray _message;

    ATTACH_THREAD(ptr, env);

	_message = (*env)->NewByteArray(env, length);
	(*env)->SetByteArrayRegion(env, _message, 0, length, (jbyte *) message);
    (*env)->CallVoidMethod(env, ptr->handler, ptr->cache->onMessageMethodId, friendnumber, _message);

	UNUSED(tox);
}

static void callback_action(Tox *tox, int32_t friendnumber, uint8_t *action, uint16_t length, void *rptr)
{
	tox_jni_globals_t *ptr = (tox_jni_globals_t *) rptr;
	JNIEnv *env;
	jbyteArray _action;

	ATTACH_THREAD(ptr, env);

	_action = (*env)->NewByteArray(env, length);
	(*env)->SetByteArrayRegion(env, _action, 0, length, (jbyte *) action);
    (*env)->CallVoidMethod(env, ptr->handler, ptr->cache->onActionMethodId, friendnumber, _action);

	UNUSED(tox);
}

static void callback_namechange(Tox *tox, int32_t friendnumber, uint8_t *newname, uint16_t length, void *rptr)
{
	tox_jni_globals_t *ptr = (tox_jni_globals_t *) rptr;
	JNIEnv *env;

	jbyteArray _newname;

	ATTACH_THREAD(ptr, env);

	_newname = (*env)->NewByteArray(env, length);
	(*env)->SetByteArrayRegion(env, _newname, 0, length, (jbyte *) newname);
    (*env)->CallVoidMethod(env, ptr->handler, ptr->cache->onNameChangeMethodId, friendnumber, _newname);

	UNUSED(tox);
}

static void callback_statusmessage(Tox *tox, int32_t friendnumber, uint8_t *newstatus, uint16_t length, void *rptr)
{
	tox_jni_globals_t *ptr = (tox_jni_globals_t *) rptr;
	JNIEnv *env;

	jbyteArray _newstatus;

	ATTACH_THREAD(ptr, env);

	_newstatus = (*env)->NewByteArray(env, length);
	(*env)->SetByteArrayRegion(env, _newstatus, 0, length, (jbyte *) newstatus);
    (*env)->CallVoidMethod(env, ptr->handler, ptr->cache->onStatusMessageMethodId, friendnumber, _newstatus);

	UNUSED(tox);
}

static void callback_userstatus(Tox *tox, int32_t friendnumber, uint8_t status, void *rptr)
{
	tox_jni_globals_t *ptr = (tox_jni_globals_t *) rptr;
	JNIEnv *env;

	jclass us_enum;
	char *enum_name;

	jfieldID fieldID;
	jobject enum_val;

	ATTACH_THREAD(ptr, env);
	us_enum = (*env)->FindClass(env, "im/tox/jtoxcore/ToxUserStatus");

	switch (status) {
		case TOX_USERSTATUS_NONE:
			enum_name = "TOX_USERSTATUS_NONE";
			break;

		case TOX_USERSTATUS_AWAY:
			enum_name = "TOX_USERSTATUS_AWAY";
			break;

		case TOX_USERSTATUS_BUSY:
			enum_name = "TOX_USERSTATUS_BUSY";
			break;

		default:
			enum_name = "TOX_USERSTATUS_INVALID";
			break;
	}

	fieldID = (*env)->GetStaticFieldID(env, us_enum, enum_name, "Lim/tox/jtoxcore/ToxUserStatus;");
	enum_val = (*env)->GetStaticObjectField(env, us_enum, fieldID);
    (*env)->CallVoidMethod(env, ptr->handler, ptr->cache->onUserStatusMethodId, friendnumber, enum_val);

	UNUSED(tox);
}

static void callback_read_receipt(Tox *tox, int32_t friendnumber, uint32_t receipt, void *rptr)
{
	tox_jni_globals_t *ptr = (tox_jni_globals_t *) rptr;
	JNIEnv *env;

	ATTACH_THREAD(ptr, env);
    (*env)->CallVoidMethod(env, ptr->handler, ptr->cache->onReadReceiptMethodId, friendnumber, receipt);

	UNUSED(tox);
}

static void callback_connectionstatus(Tox *tox, int32_t friendnumber, uint8_t newstatus, void *rptr)
{
	tox_jni_globals_t *ptr = (tox_jni_globals_t *) rptr;
	JNIEnv *env;

	jboolean _newstatus;

	ATTACH_THREAD(ptr, env);
	_newstatus = newstatus == 0 ? JNI_FALSE : JNI_TRUE;
    (*env)->CallVoidMethod(env, ptr->handler, ptr->cache->onConnectionStatusMethodId, friendnumber, _newstatus);

	UNUSED(tox);
}

static void callback_typingstatus(Tox *tox, int32_t friendnumber, uint8_t is_typing, void *rptr)
{
	tox_jni_globals_t *ptr = (tox_jni_globals_t *) rptr;
	JNIEnv *env;

	jboolean _is_typing;

	ATTACH_THREAD(ptr, env);
	_is_typing = is_typing == 0 ? JNI_FALSE : JNI_TRUE;
    (*env)->CallVoidMethod(env, ptr->handler, ptr->cache->onTypingChangeMethodId, friendnumber, _is_typing);

	UNUSED(tox);
}

static void avcallback_invite(void *tox_av, int32_t call_id, void *user_data)
{
	avcallback_helper(call_id, user_data, "ON_INVITE");

	UNUSED(tox_av);
}
static void avcallback_start(void *tox_av, int32_t call_id, void *user_data)
{
	avcallback_helper(call_id, user_data, "ON_START");

	UNUSED(tox_av);
}
static void avcallback_cancel(void *tox_av, int32_t call_id, void *user_data)
{
	avcallback_helper(call_id, user_data, "ON_CANCEL");

	UNUSED(tox_av);
}
static void avcallback_reject(void *tox_av, int32_t call_id, void *user_data)
{
	avcallback_helper(call_id, user_data, "ON_REJECT");

	UNUSED(tox_av);
}
static void avcallback_end(void *tox_av, int32_t call_id, void *user_data)
{
	avcallback_helper(call_id, user_data, "ON_END");

	UNUSED(tox_av);
}
static void avcallback_ringing(void *tox_av, int32_t call_id, void *user_data)
{
	avcallback_helper(call_id, user_data, "ON_RINGING");

	UNUSED(tox_av);
}
static void avcallback_starting(void *tox_av, int32_t call_id, void *user_data)
{
	avcallback_helper(call_id, user_data, "ON_STARTING");

	UNUSED(tox_av);
}
static void avcallback_ending(void *tox_av, int32_t call_id, void *user_data)
{
	avcallback_helper(call_id, user_data, "ON_ENDING");

	UNUSED(tox_av);
}
static void avcallback_requesttimeout(void *tox_av, int32_t call_id, void *user_data)
{
	avcallback_helper(call_id, user_data, "ON_REQUEST_TIMEOUT");

	UNUSED(tox_av);
}
static void avcallback_peertimeout(void *tox_av, int32_t call_id, void *user_data)
{
	avcallback_helper(call_id, user_data, "ON_PEER_TIMEOUT");

	UNUSED(tox_av);
}
static void avcallback_mediachange(void *tox_av, int32_t call_id, void *user_data)
{
	avcallback_helper(call_id, user_data, "ON_MEDIA_CHANGE");

	UNUSED(tox_av);
}
static void avcallback_audio(ToxAv *tox_av, int32_t call_id, int16_t *pcm_data, int pcm_data_length, void *user_data)
{
	tox_av_jni_globals_t *globals = (tox_av_jni_globals_t *) user_data;
	JNIEnv *env;
	jbyteArray output;

	ATTACH_THREAD(globals, env);
	
	//create java byte array from pcm data
	output = (*env)->NewByteArray(env, pcm_data_length*2);
	(*env)->SetByteArrayRegion(env, output, 0, pcm_data_length*2, (jbyte*) pcm_data);

	(*env)->CallVoidMethod(env, globals->handler, globals->cache->onAudioDataMethodId, call_id, output);
	(*env)->DeleteLocalRef(env, output);

	UNUSED(tox_av);
}
static void avcallback_video(ToxAv *tox_av, int32_t call_id, vpx_image_t *img, void *user_data)
{
	tox_av_jni_globals_t *globals = (tox_av_jni_globals_t *) user_data;
    JNIEnv *env;
	jmethodID handlermeth;

	ATTACH_THREAD(globals, env);

	//Create Android YV12 byte array from vpx_image
	jbyteArray output;
	int stride = ALIGN(img->d_w, 16);
	int y_size = stride * (img->d_h);
	int c_stride = ALIGN(stride / 2, 16);
	int c_size = c_stride * (img->d_h) / 2;
	int size = y_size + c_size * 2;
	int cr_offset = y_size;
	int cb_offset = y_size + c_size;
	uint8_t y;
	uint8_t v;
	uint8_t u;
	jbyte _output[size];
	unsigned long int y_coord, x_coord;

	for (y_coord = 0; y_coord < img->d_h; ++y_coord) {
		for (x_coord = 0; x_coord < img->d_w; ++x_coord) {
			y = (img->planes[0][((y_coord * img->stride[0]), x_coord)]);
			_output[(y_coord * stride) + x_coord] = y;

			if (x_coord == (x_coord / 2) * 2) {
				v = (img->planes[1][((y_coord * (img->stride[1] / 2)), x_coord / 2)]);
				_output[cr_offset + (y_coord * c_stride) + x_coord / 2] = v;
				u = (img->planes[2][((y_coord * (img->stride[2] / 2)), x_coord / 2)]);
				_output[cb_offset + (y_coord * c_stride) + x_coord / 2] = u;
			}
		}
	}

	output = (*env)->NewByteArray(env, size);
	(*env)->SetByteArrayRegion(env, output, 0, size, _output);

    (*env)->CallVoidMethod(env, globals->handler, globals->cache->onVideoDataMethodId, call_id, output, img->d_w, img->d_h);

    UNUSED(tox_av);
}

Tox_Options tox_options_to_native(JNIEnv *env, jobject tox_options)
{
    int i;
    jclass clazz;
    jfieldID ipv6enabled_fieldid;
    jfieldID udp_enabled_fieldid;
    jfieldID proxy_enabled_fieldid;
    jfieldID proxy_address_fieldid;
    jfieldID port_fieldid;
    Tox_Options tox_options_native;
    jboolean ipv6enabled;
    jboolean udp_enabled;
    jboolean proxy_enabled;
    jstring proxy_address;
    
    char *proxy_address_native;
    jint *port;


    clazz = (*env)->FindClass(env, "im/tox/jtoxcore/ToxOptions");

    ipv6enabled_fieldid   = (*env)->GetFieldID(env, clazz, "ipv6Enabled", "Z");
    udp_enabled_fieldid   = (*env)->GetFieldID(env, clazz, "udpEnabled", "Z");
    proxy_enabled_fieldid = (*env)->GetFieldID(env, clazz, "proxyEnabled", "Z");
    proxy_address_fieldid = (*env)->GetFieldID(env, clazz, "proxyAddress", "Ljava/lang/String;");
    port_fieldid          = (*env)->GetFieldID(env, clazz, "port", "I");
    
    
    ipv6enabled   = (*env)->GetBooleanField(env, tox_options, ipv6enabled_fieldid);
    udp_enabled   = (*env)->GetBooleanField(env, tox_options, udp_enabled_fieldid);
    proxy_enabled = (*env)->GetBooleanField(env, tox_options, proxy_enabled_fieldid);

    if (ipv6enabled) {
        tox_options_native.ipv6enabled = 1;
    } else {
        tox_options_native.ipv6enabled = 0;
    }
    if (udp_enabled) {
        tox_options_native.udp_disabled = 0;
    } else {
        tox_options_native.udp_disabled = 1;
    }
    if (proxy_enabled) {
        tox_options_native.proxy_enabled = 1;

        port          = (*env)->GetIntField(env, tox_options, port_fieldid);
        tox_options_native.proxy_port = port;


    	proxy_address = (*env)->GetObjectField(env, tox_options, proxy_address_fieldid);
    	proxy_address_native = (*env)->GetStringUTFChars(env, proxy_address, JNI_FALSE);
    	for (i = 0; i < sizeof(proxy_address_native); i++) {
       		tox_options_native.proxy_address[i] = proxy_address_native[i];
    	}
    	tox_options_native.proxy_address[sizeof(proxy_address_native)] = '\0';
    	(*env)->ReleaseStringUTFChars(env, proxy_address, proxy_address_native);

    } else {
    	tox_options_native.proxy_enabled = 0;
    	tox_options_native.proxy_port = 0;
    	tox_options_native.proxy_address[0] = '\0';
    }

    return tox_options_native;
}
