#include <jni.h>
#include <cstdint>
#include <cstddef>

// 7‑bit Yamaha LS9/M7CL meter ->  dB lookup

float meterValueToDb(uint8_t value) {
    static const float lookupTable[128] = {
        -95.0f, -94.0f, -93.0f, -92.0f, -91.0f, -90.0f, -89.0f, -89.0f,
        -88.0f, -87.0f, -86.0f, -85.0f, -84.0f, -83.0f, -82.0f, -81.0f,
        -80.0f, -79.0f, -78.0f, -78.0f, -77.0f, -76.0f, -75.0f, -74.0f,
        -73.0f, -73.0f, -72.0f, -72.0f, -71.0f, -70.0f, -69.0f, -68.0f,
        -67.0f, -67.0f, -66.0f, -65.0f, -64.0f, -63.0f, -62.0f, -62.0f,
        -61.0f, -60.0f, -59.0f, -58.0f, -57.0f, -56.0f, -55.0f, -55.0f,
        -54.0f, -53.0f, -52.0f, -51.0f, -50.0f, -50.0f, -49.0f, -48.0f,
        -48.0f, -47.0f, -46.0f, -45.0f, -44.0f, -43.0f, -42.0f, -42.0f,
        -41.0f, -40.0f, -39.0f, -38.0f, -37.0f, -36.0f, -35.0f, -34.0f,
        -33.0f, -32.0f, -31.0f, -30.0f, -29.0f, -28.0f, -27.0f, -26.0f,
        -25.0f, -24.0f, -23.0f, -22.0f, -21.0f, -20.0f, -19.0f, -18.0f,
        -17.0f, -16.0f, -15.0f, -14.0f, -13.0f, -12.0f, -11.0f, -10.0f,
        -9.0f,  -8.0f,  -7.0f,  -6.0f,  -5.0f,  -4.0f,  -3.0f,  -2.0f,
        -1.0f,   0.0f,   1.0f,   2.0f,   3.0f,   4.0f,   5.0f,   6.0f
    };

    return (value < 128) ? lookupTable[value] : -999.0f;
}

// Normalisation (14‑bit -> 7‑bit)

inline uint8_t normalise14(uint16_t value) {
    return static_cast<uint8_t>(value >> 7);
}

// Single‑value conversion

float resolveMeterDb(uint16_t rawValue, bool needsNormalisation) {
    uint8_t meterValue = needsNormalisation
        ? normalise14(rawValue)
        : static_cast<uint8_t>(rawValue);

    return meterValueToDb(meterValue);
}

// JNI wrapper for single‑value conversion
extern "C" JNIEXPORT jfloat JNICALL
Java_MidiControl_MeterUtils_MeterTools_meterDb(
    JNIEnv*, jclass, jint rawValue, jboolean normalise
) {
    return resolveMeterDb(
        static_cast<uint16_t>(rawValue),
        normalise == JNI_TRUE
    );
}

// Block processors

// 7‑bit block: raw[i] = 0–127
void processMeterBlock7(const uint8_t* raw, size_t count, float* outDb) {
    for (size_t i = 0; i < count; i++) {
        outDb[i] = meterValueToDb(raw[i]);
    }
}

// 14‑bit block: raw = [ll, hh, ll, hh, ...]
void processMeterBlock14(const uint8_t* raw, size_t count, float* outDb) {
    for (size_t i = 0; i < count; i++) {
        uint16_t value = (raw[i * 2 + 1] << 7) | raw[i * 2];
        uint8_t norm = normalise14(value);
        outDb[i] = meterValueToDb(norm);
    }
}

// JNI wrappers for block conversions

// 7‑bit block JNI
extern "C" JNIEXPORT void JNICALL
Java_MidiControl_MeterUtils_MeterTools_processMeterBlock7(
    JNIEnv* env, jclass,
    jbyteArray rawArray,
    jfloatArray outArray
) {
    jsize count = env->GetArrayLength(rawArray);

    jbyte* rawPtr = env->GetByteArrayElements(rawArray, nullptr);
    jfloat* outPtr = env->GetFloatArrayElements(outArray, nullptr);

    processMeterBlock7(
        reinterpret_cast<uint8_t*>(rawPtr),
        static_cast<size_t>(count),
        outPtr
    );

    env->ReleaseByteArrayElements(rawArray, rawPtr, JNI_ABORT);
    env->ReleaseFloatArrayElements(outArray, outPtr, 0);
}

// 14‑bit block JNI
extern "C" JNIEXPORT void JNICALL
Java_MidiControl_MeterUtils_MeterTools_processMeterBlock14(
    JNIEnv* env, jclass,
    jbyteArray rawArray,
    jfloatArray outArray
) {
    jsize byteCount = env->GetArrayLength(rawArray);
    size_t meterCount = byteCount / 2;

    jbyte* rawPtr = env->GetByteArrayElements(rawArray, nullptr);
    jfloat* outPtr = env->GetFloatArrayElements(outArray, nullptr);

    processMeterBlock14(
        reinterpret_cast<uint8_t*>(rawPtr),
        meterCount,
        outPtr
    );

    env->ReleaseByteArrayElements(rawArray, rawPtr, JNI_ABORT);
    env->ReleaseFloatArrayElements(outArray, outPtr, 0);
}