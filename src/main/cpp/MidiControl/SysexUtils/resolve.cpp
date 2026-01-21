// resolve_throw_on_uninit.cpp
// Minimal changes to original native resolver: no mutex/atomic.
// If native registry hasn't been initialised via updateMappings,
// resolve() throws a Java IllegalStateException and returns -1.
// Also includes a few small defensive checks to avoid NULL derefs.

#include <jni.h>
#include <cstdint>
#include <unordered_map>
#include <vector>
#include <cstdio>

namespace MidiControl {
namespace SysexUtils {

    static std::unordered_map<uint64_t, uint32_t> lookup;
    static std::vector<jint> addressBytes;
    // Simple flag (no mutex/atomic) — user requested simplest possible approach.
    static bool initialized = false;

} // namespace SysexUtils
} // namespace MidiControl

// ---------------------------------------------------------------------------
//  updateMappings: called once during initialisation
// ---------------------------------------------------------------------------
extern "C" JNIEXPORT void JNICALL
Java_MidiControl_SysexUtils_NativeSysex_updateMappings(
    JNIEnv* env,
    jclass,
    jlongArray keys,
    jintArray indexes,
    jintArray addrBytes
) {
    if (keys == nullptr || indexes == nullptr || addrBytes == nullptr) {
        // nothing to do
        return;
    }

    jsize len = env->GetArrayLength(keys);
    if (len != env->GetArrayLength(indexes)) {
        return;
    }

    // --- Load address byte metadata ---
    jsize abLen = env->GetArrayLength(addrBytes);
    if (abLen <= 0) {
        return;
    }
    MidiControl::SysexUtils::addressBytes.resize(static_cast<size_t>(abLen));
    env->GetIntArrayRegion(addrBytes, 0, abLen, MidiControl::SysexUtils::addressBytes.data());

    // --- Load key → index lookup table ---
    MidiControl::SysexUtils::lookup.clear();
    MidiControl::SysexUtils::lookup.reserve(static_cast<size_t>(len));

    jlong* keyPtr = env->GetLongArrayElements(keys, nullptr);
    jint* idxPtr  = env->GetIntArrayElements(indexes, nullptr);

    if (keyPtr == nullptr || idxPtr == nullptr) {
        if (keyPtr) env->ReleaseLongArrayElements(keys, keyPtr, JNI_ABORT);
        if (idxPtr)  env->ReleaseIntArrayElements(indexes, idxPtr, JNI_ABORT);
        return;
    }

    for (jsize i = 0; i < len; i++) {
        uint64_t key   = static_cast<uint64_t>(keyPtr[i]);
        uint32_t index = static_cast<uint32_t>(idxPtr[i]);
        MidiControl::SysexUtils::lookup[key] = index;
    }

    env->ReleaseLongArrayElements(keys, keyPtr, JNI_ABORT);
    env->ReleaseIntArrayElements(indexes, idxPtr, JNI_ABORT);

    // Mark initialized (no synchronization; caller must ensure updateMappings
    // is called before concurrent resolve() if they want to avoid races).
    MidiControl::SysexUtils::initialized = true;
}

// ---------------------------------------------------------------------------
//  resolve: fast native resolver
//  If registry not initialized, throw IllegalStateException and return -1.
// ---------------------------------------------------------------------------
extern "C" JNIEXPORT jint JNICALL
Java_MidiControl_SysexUtils_NativeSysex_resolve(
    JNIEnv* env,
    jclass,
    jbyteArray msg
) {
    // If not initialized, throw a Java exception (simple, no atomics/mutex).
    if (!MidiControl::SysexUtils::initialized) {
        jclass exClass = env->FindClass("java/lang/IllegalStateException");
        if (exClass != nullptr) {
            env->ThrowNew(exClass, "NativeSysex: registry not initialized (call updateMappings first)");
        }
        return -1;
    }

    if (msg == nullptr) {
        return -1;
    }

    jsize len = env->GetArrayLength(msg);
    if (len < 10) {
        return -1;
    }

    jbyte* data = env->GetByteArrayElements(msg, nullptr);
    if (data == nullptr) {
        return -1;
    }

    // Basic Yamaha sysex validation
    if (data[0] != (jbyte)0xF0 ||
        data[1] != (jbyte)0x43 ||
        data[len - 1] != (jbyte)0xF7) {

        env->ReleaseByteArrayElements(msg, data, JNI_ABORT);
        return -1;
    }

    // --- Build key using metadata ---
    uint64_t key = 0;

    // Defensive: check for empty addressBytes and negative indices
    if (MidiControl::SysexUtils::addressBytes.empty()) {
        env->ReleaseByteArrayElements(msg, data, JNI_ABORT);
        jclass exClass = env->FindClass("java/lang/IllegalStateException");
        if (exClass != nullptr) {
            env->ThrowNew(exClass, "NativeSysex: addressBytes metadata missing");
        }
        return -1;
    }

    for (jint b : MidiControl::SysexUtils::addressBytes) {
        if (b < 0 || b >= len) {
            env->ReleaseByteArrayElements(msg, data, JNI_ABORT);
            jclass exClass = env->FindClass("java/lang/IndexOutOfBoundsException");
            if (exClass != nullptr) {
                env->ThrowNew(exClass, "NativeSysex: address byte index out of range");
            }
            return -1;
        }
        key = (key << 8) | (static_cast<uint8_t>(data[b]) & 0x7F);
    }

    env->ReleaseByteArrayElements(msg, data, JNI_ABORT);

    // --- Lookup ---
    auto it = MidiControl::SysexUtils::lookup.find(key);
    if (it == MidiControl::SysexUtils::lookup.end()) {
        return -1;
    }

    return static_cast<jint>(it->second);
}