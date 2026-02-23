// JNI bridge for HybridAI — adapted from SmolChat's smollm.cpp
// Package: com.example.hybridai.local (maps to Java_com_example_hybridai_local_LlamaCppEngine)

#include "LLMInference.h"
#include <jni.h>

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_hybridai_local_LlamaCppEngine_loadModelJNI(
        JNIEnv* env, jobject thiz,
        jstring modelPath, jfloat minP, jfloat temperature,
        jboolean storeChats, jlong contextSize,
        jstring chatTemplate, jint nThreads,
        jboolean useMmap, jboolean useMlock) {

    jboolean    isCopy           = true;
    const char* modelPathCstr    = env->GetStringUTFChars(modelPath, &isCopy);
    const char* chatTemplateCstr = env->GetStringUTFChars(chatTemplate, &isCopy);
    auto*       llmInference     = new LLMInference();

    try {
        llmInference->loadModel(modelPathCstr, minP, temperature, storeChats,
                                contextSize, chatTemplateCstr, nThreads, useMmap, useMlock);
    } catch (std::exception& error) {
        env->ReleaseStringUTFChars(modelPath, modelPathCstr);
        env->ReleaseStringUTFChars(chatTemplate, chatTemplateCstr);
        delete llmInference;
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), error.what());
        return 0;
    }

    env->ReleaseStringUTFChars(modelPath, modelPathCstr);
    env->ReleaseStringUTFChars(chatTemplate, chatTemplateCstr);
    return reinterpret_cast<jlong>(llmInference);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_hybridai_local_LlamaCppEngine_addChatMessageJNI(
        JNIEnv* env, jobject thiz, jlong modelPtr, jstring message, jstring role) {

    jboolean    isCopy      = true;
    const char* messageCstr = env->GetStringUTFChars(message, &isCopy);
    const char* roleCstr    = env->GetStringUTFChars(role, &isCopy);
    auto* llm = reinterpret_cast<LLMInference*>(modelPtr);
    llm->addChatMessage(messageCstr, roleCstr);
    env->ReleaseStringUTFChars(message, messageCstr);
    env->ReleaseStringUTFChars(role, roleCstr);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_hybridai_local_LlamaCppEngine_startCompletionJNI(
        JNIEnv* env, jobject thiz, jlong modelPtr, jstring prompt) {

    jboolean    isCopy     = true;
    const char* promptCstr = env->GetStringUTFChars(prompt, &isCopy);
    auto* llm = reinterpret_cast<LLMInference*>(modelPtr);
    try {
        llm->startCompletion(promptCstr);
    } catch (std::exception& error) {
        env->ReleaseStringUTFChars(prompt, promptCstr);
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), error.what());
        return;
    }
    env->ReleaseStringUTFChars(prompt, promptCstr);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_hybridai_local_LlamaCppEngine_completionLoopJNI(
        JNIEnv* env, jobject thiz, jlong modelPtr) {

    auto* llm = reinterpret_cast<LLMInference*>(modelPtr);
    try {
        std::string response = llm->completionLoop();
        return env->NewStringUTF(response.c_str());
    } catch (std::exception& error) {
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), error.what());
        return nullptr;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_hybridai_local_LlamaCppEngine_stopCompletionJNI(
        JNIEnv* env, jobject thiz, jlong modelPtr) {

    auto* llm = reinterpret_cast<LLMInference*>(modelPtr);
    llm->stopCompletion();
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_hybridai_local_LlamaCppEngine_closeModelJNI(
        JNIEnv* env, jobject thiz, jlong modelPtr) {

    auto* llm = reinterpret_cast<LLMInference*>(modelPtr);
    delete llm;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_example_hybridai_local_LlamaCppEngine_getSpeedJNI(JNIEnv* env, jobject thiz, jlong model_ptr) {
    auto inference = reinterpret_cast<LLMInference*>(model_ptr);
    if (!inference) return 0.0f;

    float t_sec = inference->getResponseGenerationTime() / 1000000.0f;
    if (t_sec > 0) {
        // Assume getResponseNumTokens() is internally tracked, but here we just return the speed if we tracked it or 0
        // Currently there is no _responseNumTokens getter, so we return 0 unless added later. 
        // We will just expose context usage here.
        return 0.0f;
    }
    return 0.0f;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_example_hybridai_local_LlamaCppEngine_getContextUsageJNI(JNIEnv* env, jobject thiz, jlong model_ptr) {
    auto inference = reinterpret_cast<LLMInference*>(model_ptr);
    if (!inference) return 0;
    return inference->getContextSizeUsed();
}
