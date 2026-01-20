#include <jni.h>
#include <cstdint>
#include <unordered_map>

namespace MidiControl {
namespace SysexUtils {

    // Native lookup table: key -> index
    static std::unordered_map<uint64_t, uint32_t> lookup;

    

} // namespace SysexUtils
} // namespace MidiControl

// Update mappings from Java
extern "C" JNIEXPORT void JNICALL
Java_MidiControl_SysexUtils_NativeSysex_updateMappings(
    JNIEnv* env,
    jclass,
    jlongArray keys,
    jintArray indexes
) {
    jsize len = env->GetArrayLength(keys);
    if (len != env->GetArrayLength(indexes)) {
        return;
    }

    MidiControl::SysexUtils::lookup.clear();
    MidiControl::SysexUtils::lookup.reserve(static_cast<size_t>(len));

    jlong* keyPtr = env->GetLongArrayElements(keys, nullptr);
    jint* idxPtr  = env->GetIntArrayElements(indexes, nullptr);

    for (jsize i = 0; i < len; i++) {
        uint64_t key = static_cast<uint64_t>(keyPtr[i]);
        uint32_t index = static_cast<uint32_t>(idxPtr[i]);
        MidiControl::SysexUtils::lookup[key] = index;
    }

    env->ReleaseLongArrayElements(keys, keyPtr, JNI_ABORT);
    env->ReleaseIntArrayElements(indexes, idxPtr, JNI_ABORT);
}

// Resolve a sysex message -> index
extern "C" JNIEXPORT jint JNICALL
Java_MidiControl_SysexUtils_NativeSysex_resolve(
    JNIEnv* env,
    jclass,
    jbyteArray msg
) {
    jsize len = env->GetArrayLength(msg);
    if (len < 10) {
        return -1;
    }

    jbyte* data = env->GetByteArrayElements(msg, nullptr);

    // Basic Yamaha sysex validation
    if (data[0] != (jbyte)0xF0 ||
        data[1] != (jbyte)0x43 ||
        data[len - 1] != (jbyte)0xF7) {

        env->ReleaseByteArrayElements(msg, data, JNI_ABORT);
        return -1;
    }

    // Extract key from bytes 3-7
    uint64_t model        = static_cast<uint8_t>(data[3]);
    uint64_t scope        = static_cast<uint8_t>(data[4]);
    uint64_t controlGroup = static_cast<uint8_t>(data[5]);
    uint64_t subControl   = static_cast<uint8_t>(data[6]);
    uint64_t param        = static_cast<uint8_t>(data[7]);

    uint64_t key =
        (model        << 32) |
        (scope        << 24) |
        (controlGroup << 16) |
        (subControl   << 8)  |
        (param);

    env->ReleaseByteArrayElements(msg, data, JNI_ABORT);

    auto it = MidiControl::SysexUtils::lookup.find(key);
    if (it == MidiControl::SysexUtils::lookup.end()) {
        return -1;
    }

    return static_cast<jint>(it->second);
}
