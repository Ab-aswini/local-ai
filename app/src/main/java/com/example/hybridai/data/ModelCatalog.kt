package com.example.hybridai.data

data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val sizeLabel: String,
    val ramRequired: String,
    val downloadUrl: String,
    val filename: String,
    val warningLabel: String = ""
)

object ModelCatalog {
    val models = listOf(
        ModelInfo(
            id = "tinyllama_1b",
            name = "TinyLlama 1.1B",
            description = "Fastest possible. Perfect for very low-end phones (2 GB RAM).",
            sizeLabel = "650 MB",
            ramRequired = "2 GB RAM",
            downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            filename = "tinyllama-1.1b-q4.gguf"
        ),
        ModelInfo(
            id = "llama3_1b",
            name = "Llama 3.2 1B",
            description = "Fast and capable. Best for 4 GB RAM phones. ⭐ Recommended.",
            sizeLabel = "800 MB",
            ramRequired = "2 GB RAM",
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            filename = "llama3.2-1b-q4.gguf"
        ),
        ModelInfo(
            id = "qwen25_1b",
            name = "Qwen 2.5 1.5B",
            description = "Multilingual (English + Hindi + Chinese). Great all-rounder.",
            sizeLabel = "950 MB",
            ramRequired = "2 GB RAM",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
            filename = "qwen2.5-1.5b-q4.gguf"
        ),
        ModelInfo(
            id = "gemma2_2b",
            name = "Gemma 2 2B",
            description = "Google's model. Great balance of speed and quality.",
            sizeLabel = "1.6 GB",
            ramRequired = "3 GB RAM",
            downloadUrl = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf",
            filename = "gemma2-2b-q4.gguf"
        ),
        ModelInfo(
            id = "phi3_mini",
            name = "Phi-3 Mini 3.8B",
            description = "Microsoft's compact model. Best reasoning in its size class.",
            sizeLabel = "2.2 GB",
            ramRequired = "4 GB RAM",
            downloadUrl = "https://huggingface.co/bartowski/Phi-3-mini-4k-instruct-GGUF/resolve/main/Phi-3-mini-4k-instruct-Q4_K_M.gguf",
            filename = "phi3-mini-q4.gguf",
            warningLabel = "⚠️ Needs 4 GB free RAM"
        )
    )
}
