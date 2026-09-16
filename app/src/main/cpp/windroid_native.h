#ifndef WINDROID_NATIVE_H
#define WINDROID_NATIVE_H

#include <jni.h>
#include <string>
#include <vector>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Struct representing hardware capability information probed directly
 * via Linux /sys and Vulkan loaders on Android ARM64 devices.
 */
struct DeviceHardwareInfo {
    bool hasNeon;
    bool hasAtomics;
    bool hasCrypto;
    bool hasFp16;
    bool isQualcommAdreno;
    char gpuRenderer[128];
    int vulkanApiVersion;
    int maxMemoryMb;
};

/*
 * JNI exported functions for WinDroid Native Bridge
 */
JNIEXPORT jstring JNICALL
Java_org_windroid_core_nativebridge_NativeBridge_getCpuArchitecture(JNIEnv *env, jobject thiz);

JNIEXPORT jint JNICALL
Java_org_windroid_core_nativebridge_NativeBridge_checkVulkanSupport(JNIEnv *env, jobject thiz);

JNIEXPORT jstring JNICALL
Java_org_windroid_core_nativebridge_NativeBridge_detectGpuRenderer(JNIEnv *env, jobject thiz);

JNIEXPORT jint JNICALL
Java_org_windroid_core_nativebridge_NativeBridge_createPtySession(JNIEnv *env, jobject thiz, jobjectArray cmd_args, jobjectArray env_vars, jstring work_dir);

JNIEXPORT jint JNICALL
Java_org_windroid_core_nativebridge_NativeBridge_sendInputEvent(JNIEnv *env, jobject thiz, jint event_type, jint code, jint value);

JNIEXPORT jint JNICALL
Java_org_windroid_core_nativebridge_NativeBridge_createSharedBuffer(JNIEnv *env, jobject thiz, jstring name, jlong size);

JNIEXPORT jint JNICALL
Java_org_windroid_core_nativebridge_NativeBridge_closePtySession(JNIEnv *env, jobject thiz, jint pty_fd);

#ifdef __cplusplus
}
#endif

#endif // WINDROID_NATIVE_H
