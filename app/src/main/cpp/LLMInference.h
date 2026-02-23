#pragma once
#include "llama.h"
#include "chat.h"
#include "common.h"
#include <string>
#include <vector>

class LLMInference {
    llama_context* _ctx    = nullptr;
    llama_model*   _model  = nullptr;
    llama_sampler* _sampler = nullptr;
    llama_token    _currToken;
    llama_batch*   _batch  = nullptr;

    // Chat history stored as role/content pairs
    struct ChatMsg { std::string role; std::string content; };
    std::vector<ChatMsg>     _chatHistory;
    std::vector<llama_token> _promptTokens;
    std::string              _chatTemplate; // empty = use model default

    std::string _response;
    std::string _cacheResponseTokens;
    bool        _storeChats = false;

    int64_t _responseGenerationTime = 0;
    long    _responseNumTokens      = 0;
    int     _nCtxUsed               = 0;

    bool _isValidUtf8(const char* s);

  public:
    void loadModel(const char* modelPath, float minP, float temperature,
                   bool storeChats, long contextSize, const char* chatTemplate,
                   int nThreads, bool useMmap, bool useMlock);

    void addChatMessage(const char* message, const char* role);

    float getResponseGenerationTime() const;
    int   getContextSizeUsed()        const;

    void        startCompletion(const char* query);
    std::string completionLoop();
    void        stopCompletion();

    ~LLMInference();
};
