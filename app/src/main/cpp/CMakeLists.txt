# CMakeLists.txt for Hybrid AI Assistant
# This script compiles the llama.cpp backend for Android NDK usage.

cmake_minimum_required(VERSION 3.22.1)

project("hybridai-llama")

# 1. Define the JNI Bridge library
add_library(
        hybridai-llama
        SHARED
        native-lib.cpp
)

# 2. Setup Include Directories
# IMPORTANT: This assumes you will drop the 'llama.cpp' source code here.
# Download from: https://github.com/ggerganov/llama.cpp
# Expected paths:
#   app/src/main/cpp/llama.cpp
#   app/src/main/cpp/llama.h
#   app/src/main/cpp/ggml.c
#   app/src/main/cpp/ggml.h
include_directories(${CMAKE_CURRENT_SOURCE_DIR})

# 3. Add llama.cpp source files if they exist
# Note: In a real environment, uncomment these once the files are present
# target_sources(hybridai-llama PRIVATE
#     llama.cpp
#     ggml.c
#     ggml-alloc.c
#     ggml-backend.c
#     ggml-quants.c
# )

# 4. CPU Optimization Flags for Low-End Androids (~4GB RAM)
# SmolChat methodology: Maximize ARM NEON, avoid OpenCL/Vulkan overhead for cheap phones.
target_compile_definitions(hybridai-llama PRIVATE
    GGML_USE_CPU
)

# 5. Link standard Android Android logging and math libraries
find_library(log-lib log)

target_link_libraries(
        hybridai-llama
        ${log-lib}
        m
)
