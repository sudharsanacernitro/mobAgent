package com.example.myapplication

import com.rk.terminal.setupAlpine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AlpineWrapper {

    @JvmStatic
    fun setupAlpineAsync(
        onProgress: (Float) -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                setupAlpine(
                    onProgress = onProgress,
                    onComplete = onComplete,
                    onError = onError
                )
            } catch (e: Exception) {
                onError(e)
            }
        }
    }
}