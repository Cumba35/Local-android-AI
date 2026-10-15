package com.localllm.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Kotlin wrapper around the native llama.cpp JNI bridge.
 */
class LlamaEngine {

    // ── Token callback interface (implemented in JNI) ─────────────────────
    fun interface TokenCallback {
        fun onToken(piece: String)
    }

    // ── State ─────────────────────────────────────────────────────────────
    var isLoaded = false
        private set

    // ── Public API ────────────────────────────────────────────────────────

    suspend fun loadModel(
        modelPath: String,
        nGpuLayers: Int = 99,   // 99 = offload everything to GPU if available
        nCtx: Int = 4096
    ): Boolean = withContext(Dispatchers.IO) {
        val ok = nativeLoad(modelPath, nGpuLayers, nCtx)
        isLoaded = ok
        ok
    }

    suspend fun completion(
        prompt: String,
        nPredict: Int = 512,
        onToken: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        nativeCompletion(prompt, nPredict, TokenCallback { onToken(it) })
    }

    fun stop() = nativeStop()

    fun free() {
        nativeFree()
        isLoaded = false
    }

    fun modelInfo(): String = nativeModelInfo()

    
    /**
     * Construye una conversación con el formato ChatML de LFM2.5.
     */
    fun buildPrompt(
        messages: List<ChatMessage>,
        systemPrompt: String
    ): String {
        val sb = StringBuilder()

        sb.append("<|startoftext|>")
        sb.append("<|im_start|>system\n")
        sb.append(systemPrompt)
        sb.append("<|im_end|>\n")

        for (msg in messages) {
            val role = if (msg.isUser) "user" else "assistant"

            sb.append("<|im_start|>")
            sb.append(role)
            sb.append("\n")
            sb.append(msg.text)
            sb.append("<|im_end|>\n")
        }

        sb.append("<|im_start|>assistant\n")

        return sb.toString()
    }

    

    // ── Native declarations ───────────────────────────────────────────────
    private external fun nativeLoad(path: String, nGpuLayers: Int, nCtx: Int): Boolean
    private external fun nativeCompletion(prompt: String, nPredict: Int, callback: TokenCallback)
    private external fun nativeStop()
    private external fun nativeFree()
    private external fun nativeModelInfo(): String

    companion object {
        init {
            System.loadLibrary("localllm")
        }
    }
}
