package com.spcrey.blog.tools

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object MessageReceiving {
    interface Listener {
        suspend fun countComplete()
    }
    private var listener: Listener? = null

    fun setListener(listener: Listener) {
        this.listener = listener
    }
    fun clearListener() {
        listener = null
    }
    private val scope = CoroutineScope(Dispatchers.IO)
    var max_count = 5
    var current_count = 0
    fun run() {
        scope.launch {
            while (true) {
                if (current_count >= max_count) {
                    listener?.countComplete()
                    current_count = 0
                }
                delay(1000)
                current_count += 1
            }
        }
    }
}