#include <cstdint>
#include <jni.h>
extern "C" {
    void engine_start();
    void engine_stop();
    void engine_clear_chain();
    int  engine_add_node(const char* type);
    void engine_set_param(int index, int paramId, float value);
    float engine_get_input_level();
    void engine_set_preferred_input_device_id(int32_t deviceId);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_lowlatencymonitor_audio_AudioEngine_start(JNIEnv*, jobject){ engine_start(); }

extern "C" JNIEXPORT void JNICALL
Java_com_example_lowlatencymonitor_audio_AudioEngine_stop(JNIEnv*, jobject){ engine_stop(); }

extern "C" JNIEXPORT void JNICALL
Java_com_example_lowlatencymonitor_audio_AudioEngine_clearChain(JNIEnv*, jobject){ engine_clear_chain(); }

extern "C" JNIEXPORT jint JNICALL
Java_com_example_lowlatencymonitor_audio_AudioEngine_addNode(JNIEnv* env, jobject, jstring jtype){
    const char* type = env->GetStringUTFChars(jtype, nullptr);
    int idx = engine_add_node(type);
    env->ReleaseStringUTFChars(jtype, type);
    return idx;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_lowlatencymonitor_audio_AudioEngine_setParam(JNIEnv*, jobject, jint idx, jint pid, jfloat v){
    engine_set_param(idx, pid, v);
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_example_lowlatencymonitor_audio_AudioEngine_getInputLevel(JNIEnv*, jobject){
    return engine_get_input_level();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_lowlatencymonitor_audio_AudioEngine_setPreferredInputDeviceId(JNIEnv*, jobject, jint deviceId){
    engine_set_preferred_input_device_id(deviceId);
}
