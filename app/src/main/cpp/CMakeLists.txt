cmake_minimum_required(VERSION 3.22.1)
project("hybridai")

# ── llama.cpp subdir (cloned into app/src/main/cpp/llama.cpp) ──────────────
add_subdirectory(llama.cpp llama.cpp_build)

set(LLAMA_DIR  ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)
set(GGML_DIR   ${LLAMA_DIR}/ggml)
set(COMMON_DIR ${LLAMA_DIR}/common)

# Hide internal symbols, keep binary small
target_compile_options(llama PUBLIC
    -fvisibility=hidden -fvisibility-inlines-hidden
    -ffunction-sections -fdata-sections
)
target_link_options(llama PRIVATE
    -Wl,--gc-sections -flto -Wl,--exclude-libs,ALL
)

# ── Our JNI + C++ inference wrapper ────────────────────────────────────────
add_library(hybridai SHARED
    hybridai.cpp
    LLMInference.cpp
)

target_include_directories(hybridai PUBLIC
    ${COMMON_DIR}
    ${GGML_DIR}/include
    ${GGML_DIR}/src
    ${GGML_DIR}/src/ggml-cpu
    ${LLAMA_DIR}/include
    ${LLAMA_DIR}/vendor          # nlohmann/json, required by peg-parser.h
    ${LLAMA_DIR}/src/../include
    ${GGML_DIR}/src/../include
)

target_compile_options(hybridai PUBLIC
    -fvisibility=hidden -fvisibility-inlines-hidden
    -ffunction-sections -fdata-sections
    -DGGML_USE_CPU -O3
)

target_link_libraries(hybridai
    android log llama
    # NOTE: 'common' is NOT linked here — the common static lib depends on
    # cpp-httplib (HTTP networking) which has no NDK equivalent and causes
    # "unable to find library -lcommon". Headers are still resolved via
    # target_include_directories above.
)

target_link_options(hybridai PRIVATE
    -Wl,--gc-sections -flto -Wl,--exclude-libs,ALL
)
