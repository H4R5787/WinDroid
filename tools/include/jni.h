#ifndef _JAVASOFT_JNI_H_
#define _JAVASOFT_JNI_H_

#include <stdint.h>
#include <stddef.h>

#ifndef JNIEXPORT
#if defined(_WIN32)
#define JNIEXPORT __declspec(dllexport)
#else
#define JNIEXPORT __attribute__ ((visibility ("default")))
#endif
#endif

#ifndef JNICALL
#define JNICALL
#endif

typedef uint8_t  jboolean;
typedef int8_t   jbyte;
typedef uint16_t jchar;
typedef int16_t  jshort;
typedef int32_t  jint;
typedef int64_t  jlong;
typedef float    jfloat;
typedef double   jdouble;
typedef jint     jsize;

#ifdef __cplusplus
class _jobject {};
class _jclass : public _jobject {};
class _jstring : public _jobject {};
class _jarray : public _jobject {};
class _jobjectArray : public _jarray {};

typedef _jobject*       jobject;
typedef _jclass*        jclass;
typedef _jstring*       jstring;
typedef _jarray*        jarray;
typedef _jobjectArray*  jobjectArray;
#else
typedef void* jobject;
typedef void* jclass;
typedef void* jstring;
typedef void* jarray;
typedef void* jobjectArray;
#endif

struct JNINativeInterface_;

#ifdef __cplusplus
struct JNIEnv_;
typedef struct JNIEnv_ JNIEnv;
#else
typedef const struct JNINativeInterface_* JNIEnv;
#endif

struct JNINativeInterface_ {
    void* reserved0;
    void* reserved1;
    void* reserved2;
    void* reserved3;
    jstring (*NewStringUTF)(JNIEnv*, const char*);
    const char* (*GetStringUTFChars)(JNIEnv*, jstring, jboolean*);
    void (*ReleaseStringUTFChars)(JNIEnv*, jstring, const char*);
    jsize (*GetArrayLength)(JNIEnv*, jarray);
    jobject (*GetObjectArrayElement)(JNIEnv*, jobjectArray, jsize);
};

#ifdef __cplusplus
struct JNIEnv_ {
    const struct JNINativeInterface_* functions;

    jstring NewStringUTF(const char* utf) {
        return functions ? functions->NewStringUTF(this, utf) : nullptr;
    }
    const char* GetStringUTFChars(jstring str, jboolean* isCopy) {
        return functions ? functions->GetStringUTFChars(this, str, isCopy) : nullptr;
    }
    void ReleaseStringUTFChars(jstring str, const char* chars) {
        if (functions) functions->ReleaseStringUTFChars(this, str, chars);
    }
    jsize GetArrayLength(jarray array) {
        return functions ? functions->GetArrayLength(this, array) : 0;
    }
    jobject GetObjectArrayElement(jobjectArray array, jsize index) {
        return functions ? functions->GetObjectArrayElement(this, array, index) : nullptr;
    }
};
#endif

#endif /* _JAVASOFT_JNI_H_ */
