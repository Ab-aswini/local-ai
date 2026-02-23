// LLMInference.cpp
// Uses ONLY llama.h — no common.h, no chat.h.
// Replaces:
//   common_tokenize          -> llama_tokenize
//   common_token_to_piece    -> llama_token_to_piece
//   common_chat_templates_*  -> llama_chat_apply_template (raw C API)

#include "LLMInference.h"
#include <android/log.h>
#include <cstring>
#include <iomanip>
#include <iostream>
#include <sstream>

#define TAG  "[HybridAI-Cpp]"
#define LOGi(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGe(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// ── Removed custom token logic since common API is restored ──

// ── Load / Init ───────────────────────────────────────────────────────────────

void LLMInference::loadModel(const char* model_path, float minP, float temperature,
                              bool storeChats, long contextSize, const char* chatTemplate,
                              int nThreads, bool useMmap, bool useMlock) {
    LOGi("loading model: %s", model_path);

    ggml_backend_load_all();

    llama_model_params model_params = llama_model_default_params();
    model_params.use_mmap  = useMmap;
    model_params.use_mlock = useMlock;
    _model = llama_model_load_from_file(model_path, model_params);
    if (!_model) {
        LOGe("failed to load model from %s", model_path);
        throw std::runtime_error("llama_model_load_from_file() failed");
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx     = (uint32_t)contextSize;
    ctx_params.n_batch   = (uint32_t)contextSize;
    ctx_params.n_threads = nThreads;
    ctx_params.no_perf   = true;
    _ctx = llama_init_from_model(_model, ctx_params);
    if (!_ctx) {
        LOGe("llama_init_from_model() returned null");
        throw std::runtime_error("llama_init_from_model() returned null");
    }

    llama_sampler_chain_params sp = llama_sampler_chain_default_params();
    sp.no_perf = true;
    _sampler = llama_sampler_chain_init(sp);
    llama_sampler_chain_add(_sampler, llama_sampler_init_temp(temperature));
    llama_sampler_chain_add(_sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    _chatHistory.clear();
    _chatTemplate = (chatTemplate && chatTemplate[0] != '\0') ? chatTemplate : "";
    _storeChats   = storeChats;
    LOGi("model loaded successfully");
}

// ── Chat ──────────────────────────────────────────────────────────────────────

void LLMInference::addChatMessage(const char* message, const char* role) {
    _chatHistory.push_back({role, message});
}

float LLMInference::getResponseGenerationTime() const {
    return (float)_responseNumTokens / (_responseGenerationTime / 1e6f);
}

int LLMInference::getContextSizeUsed() const {
    return _nCtxUsed;
}

// ── Start completion ───────────────────────────────────────────────────────────

void LLMInference::startCompletion(const char* query) {
    _responseGenerationTime = 0;
    _responseNumTokens      = 0;
    addChatMessage(query, "user");

    // Build a contiguous buffer of llama_chat_message pointers for the API.
    std::vector<llama_chat_message> msgs;
    msgs.reserve(_chatHistory.size());
    for (const auto& m : _chatHistory) {
        msgs.push_back({m.role.c_str(), m.content.c_str()});
    }

    // apply the chat-template
    std::vector<common_chat_msg> messages;
    for (const auto& m : _chatHistory) {
        messages.push_back({m.role, m.content});
    }

    common_chat_templates_inputs inputs;
    inputs.use_jinja = true;
    inputs.messages  = messages;
    auto templates   = common_chat_templates_init(_model, _chatTemplate.c_str());
    std::string prompt = common_chat_templates_apply(templates.get(), inputs).prompt;

    const llama_vocab* vocab = llama_model_get_vocab(_model);
    _promptTokens = common_tokenize(vocab, prompt, /*add_special=*/true, /*parse_special=*/true);
    LOGi("prompt tokenized: %d tokens", (int)_promptTokens.size());

    _batch          = new llama_batch();
    _batch->token   = _promptTokens.data();
    _batch->n_tokens = (int32_t)_promptTokens.size();
}

// ── Completion loop ───────────────────────────────────────────────────────────

bool LLMInference::_isValidUtf8(const char* response) {
    if (!response) return true;
    const unsigned char* b = (const unsigned char*)response;
    int num;
    while (*b != 0x00) {
        if      ((*b & 0x80) == 0x00) num = 1;
        else if ((*b & 0xE0) == 0xC0) num = 2;
        else if ((*b & 0xF0) == 0xE0) num = 3;
        else if ((*b & 0xF8) == 0xF0) num = 4;
        else return false;
        b += 1;
        for (int i = 1; i < num; ++i) {
            if ((*b & 0xC0) != 0x80) return false;
            b += 1;
        }
    }
    return true;
}

std::string LLMInference::completionLoop() {
    uint32_t contextSize = llama_n_ctx(_ctx);
    _nCtxUsed = llama_memory_seq_pos_max(llama_get_memory(_ctx), 0) + 1;
    if (_nCtxUsed + _batch->n_tokens > (int)contextSize) {
        throw std::runtime_error("context size reached");
    }

    auto start = ggml_time_us();
    if (llama_decode(_ctx, *_batch) < 0) {
        throw std::runtime_error("llama_decode() failed");
    }

    _currToken = llama_sampler_sample(_sampler, _ctx, -1);
    if (llama_vocab_is_eog(llama_model_get_vocab(_model), _currToken)) {
        addChatMessage(_response.c_str(), "assistant");
        _response.clear();
        return "[EOG]";
    }

    std::string piece = common_token_to_piece(_ctx, _currToken, true);
    auto end = ggml_time_us();
    _responseGenerationTime += (end - start);
    _responseNumTokens      += 1;
    _cacheResponseTokens    += piece;

    _batch->token    = &_currToken;
    _batch->n_tokens = 1;

    if (_isValidUtf8(_cacheResponseTokens.c_str())) {
        _response += _cacheResponseTokens;
        std::string valid = _cacheResponseTokens;
        _cacheResponseTokens.clear();
        return valid;
    }
    return "";
}

// ── Stop / Cleanup ────────────────────────────────────────────────────────────

void LLMInference::stopCompletion() {
    if (_storeChats) addChatMessage(_response.c_str(), "assistant");
    _response.clear();
}

LLMInference::~LLMInference() {
    llama_sampler_free(_sampler);
    llama_free(_ctx);
    llama_model_free(_model);
    delete _batch;
}
