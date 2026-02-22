package com.example.hybridai.data

/**
 * Catalog of downloadable Small Language Models (SLMs).
 * All models are in GGUF format, compatible with llama.cpp.
 */
data class ModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val sizeLabel: String,       // e.g. "800 MB"
    val ramRequired: String,     // e.g. "2 GB RAM min"
    val downloadUrl: String,
    val filename: String         // saved filename on disk
)

object ModelCatalog {
    val models = listOf(
        ModelInfo(
            id = "llama3_1b",
            name = "Llama 3.2 1B",
            description = "Fast, lightweight. Best for simple Q&A on 4GB RAM phones.",
            sizeLabel = "800 MB",
            ramRequired = "2 GB RAM",
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            filename = "llama3.2-1b-q4.gguf"
        ),
        ModelInfo(
            id = "gemma2_2b",
            name = "Gemma 2 2B",
            description = "Google's efficient model. Great balance of speed and quality.",
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
            filename = "phi3-mini-q4.gguf"
        )
    )
}
