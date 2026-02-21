#include <jni.h>
#include <string>
#include <android/log.h>

#define TAG "LlamaCppJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// IMPORTANT: Uncomment these when llama.cpp is added to the directory
// #include "llama.h"
// #include "ggml.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_hybridai_local_LlamaCppEngine_loadGgufModelJNI(
        JNIEnv* env,
        jobject /* this */,
        jstring modelPath,
        jboolean useMmap) {
    
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("Loading model from path: %s. mmap requested: %d", path, useMmap);

    /* --- llama.cpp implementation skeleton --- 
    
    llama_backend_init(false);
    
    llama_model_params model_params = llama_model_default_params();
    model_params.use_mmap = useMmap; // CRITICAL for 4GB RAM devices
    
    llama_model* model = llama_load_model_from_file(path, model_params);
    if (model == nullptr) {
        LOGE("Failed to load model.");
        env->ReleaseStringUTFChars(modelPath, path);
        return 0;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 2048; // Keep context small for memory
    llama_context* ctx = llama_new_context_with_model(model, ctx_params);
    
    // In a real implementation, you would store `model` and `ctx` in a struct
    // and return the pointer address as jlong.
    auto* contextWrapper = new MyContextWrapper(model, ctx);
    jlong contextId = reinterpret_cast<jlong>(contextWrapper);
    
    -------------------------------------------*/
    
    env->ReleaseStringUTFChars(modelPath, path);
    
    // Returning dummy ID for now
    return 123456789L;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_hybridai_local_LlamaCppEngine_freeGgufModelJNI(
        JNIEnv* env,
        jobject /* this */,
        jlong contextId) {
    
    LOGI("Freeing model memory for contextId: %lld", contextId);

    /* --- llama.cpp implementation skeleton ---
    
    auto* wrapper = reinterpret_cast<MyContextWrapper*>(contextId);
    if (wrapper != nullptr) {
        llama_free(wrapper->ctx);
        llama_free_model(wrapper->model);
        llama_backend_free();
        delete wrapper;
    }
    
    -------------------------------------------*/
}
