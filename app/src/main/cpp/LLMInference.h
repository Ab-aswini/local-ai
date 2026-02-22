#pragma once
#include "chat.h"
#include "common.h"
#include "llama.h"
#include <string>
#include <vector>

class LLMInference {
    llama_context* _ctx;
    llama_model*   _model;
    llama_sampler* _sampler;
    llama_token    _currToken;
    llama_batch*   _batch;

    llama_batch g_batch;

    std::vector<llama_chat_message> _messages;
    std::vector<char>               _formattedMessages;
    std::vector<llama_token>        _promptTokens;
    const char*                     _chatTemplate;

    std::string _response;
    std::string _cacheResponseTokens;
    bool        _storeChats;

    int64_t _responseGenerationTime = 0;
    long    _responseNumTokens      = 0;
    int     _nCtxUsed               = 0;

    bool _isValidUtf8(const char* response);

  public:
    void loadModel(const char* modelPath, float minP, float temperature,
                   bool storeChats, long contextSize, const char* chatTemplate,
                   int nThreads, bool useMmap, bool useMlock);

    void addChatMessage(const char* message, const char* role);

    float getResponseGenerationTime() const;
    int   getContextSizeUsed() const;

    void        startCompletion(const char* query);
    std::string completionLoop();
    void        stopCompletion();

    std::string benchModel(int pp, int tg, int pl, int nr);

    ~LLMInference();
};
