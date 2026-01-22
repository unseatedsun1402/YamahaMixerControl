
// MeterTools.cpp
// Native JNI implementation for MidiControl.UserInterface.Meter.MeterTools
// Exports:
//   long convertSingle(byte[] raw, int bytesPer)
//   void convertBlock(byte[] raw, int bytesPer, long[] out)

#include <jni.h>
#include <cstdint>
#include <cstddef>
#include <cmath>
#include <climits>

//
// 7‑bit Yamaha meter law lookup (shared for all mixers with 1‑byte meters)
//
static const float YAMAHA_7BIT_DB[128] = {
    -95,-94,-93,-92,-91,-90,-89,-89,
    -88,-87,-86,-85,-84,-83,-82,-81,
    -80,-79,-78,-78,-77,-76,-75,-74,
    -73,-73,-72,-72,-71,-70,-69,-68,
    -67,-67,-66,-65,-64,-63,-62,-62,
    -61,-60,-59,-58,-57,-56,-55,-55,
    -54,-53,-52,-51,-50,-50,-49,-48,
    -48,-47,-46,-45,-44,-43,-42,-42,
    -41,-40,-39,-38,-37,-36,-35,-34,
    -33,-32,-31,-30,-29,-28,-27,-26,
    -25,-24,-23,-22,-21,-20,-19,-18,
    -17,-16,-15,-14,-13,-12,-11,-10,
    -9,-8,-7,-6,-5,-4,-3,-2,
    -1,0,1,2,3,4,5,6
};

static inline long db7_to_centi(uint8_t v) {
    if (v >= 112) v = 111;
    float db = YAMAHA_7BIT_DB[v];
    return (long)std::llround(db * 100.0);
}

//
// Helpers for 2×7‑bit Yamaha values (01V96 etc.)
//
static inline uint16_t raw14_from_two7(const uint8_t* b) {
    return (uint16_t)(((b[0] & 0x7F) << 7) | (b[1] & 0x7F));
}

//
// Correct 14‑bit → 7‑bit normalisation for 01V96
// Range: 0..4368 = usable
// 0x1FFF = clip
//
static inline uint8_t normalise14(uint16_t v14) {
    if (v14 >= 0x1FFF)  // clip flag
        return 127;

    if (v14 > 4368)
        v14 = 4368;

    float scaled = (float)v14 * (127.0f / 4368.0f);
    if (scaled < 0) scaled = 0;
    if (scaled > 127) scaled = 127;

    return (uint8_t)(scaled);
}

//
// JNI: convert a single meter value
//
extern "C" JNIEXPORT jlong JNICALL
Java_MidiControl_UserInterface_Meter_MeterTools_convertSingle(
    JNIEnv* env, jclass,
    jbyteArray rawArray,
    jint bytesPer
) {
    if (!rawArray || bytesPer < 1 || bytesPer > 2)
        return LONG_MIN;

    jsize len = env->GetArrayLength(rawArray);
    if (len < bytesPer)
        return LONG_MIN;

    jbyte* rawPtr = env->GetByteArrayElements(rawArray, nullptr);
    if (!rawPtr)
        return LONG_MIN;

    const uint8_t* raw = reinterpret_cast<const uint8_t*>(rawPtr);
    long result = LONG_MIN;

    if (bytesPer == 1) {
        result = db7_to_centi(raw[0] & 0x7F);
    } else {
        uint16_t v14 = raw14_from_two7(raw);
        uint8_t v7 = normalise14(v14);
        result = db7_to_centi(v7);
    }

    env->ReleaseByteArrayElements(rawArray, rawPtr, JNI_ABORT);
    return result;
}

//
// JNI: convert a block of meter values
//
extern "C" JNIEXPORT void JNICALL
Java_MidiControl_UserInterface_Meter_MeterTools_convertBlock(
    JNIEnv* env, jclass,
    jbyteArray rawArray,
    jint bytesPer,
    jlongArray outArray
) {
    if (!rawArray || !outArray || bytesPer < 1 || bytesPer > 2)
        return;

    jsize byteCount = env->GetArrayLength(rawArray);
    if (byteCount <= 0)
        return;

    jsize count = byteCount / bytesPer;
    jsize outLen = env->GetArrayLength(outArray);
    if (outLen < count)
        count = outLen;

    jbyte* rawPtr = env->GetByteArrayElements(rawArray, nullptr);
    if (!rawPtr)
        return;

    jlong* outPtr = env->GetLongArrayElements(outArray, nullptr);
    if (!outPtr) {
        env->ReleaseByteArrayElements(rawArray, rawPtr, JNI_ABORT);
        return;
    }

    const uint8_t* raw = reinterpret_cast<const uint8_t*>(rawPtr);

    for (jsize i = 0; i < count; ++i) {
        const uint8_t* p = raw + (i * bytesPer);
        long val;

        if (bytesPer == 1) {
            val = db7_to_centi(p[0] & 0x7F);
        } else {
            uint16_t v14 = raw14_from_two7(p);
            uint8_t v7 = normalise14(v14);
            val = db7_to_centi(v7);
        }

        outPtr[i] = (jlong)val;
    }

    env->ReleaseByteArrayElements(rawArray, rawPtr, JNI_ABORT);
    env->ReleaseLongArrayElements(outArray, outPtr, 0);
}
