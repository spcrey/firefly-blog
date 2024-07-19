package com.spcrey.blog.tools

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object CodeTimer {
    interface Listener {
        suspend fun resumeTimeChange(resumeTime: Int)
        suspend fun complete()
    }

    private const val MAX_COUNT = 10
    private var resumeTime: Int = 0
    private val scope = CoroutineScope(Dispatchers.IO)
    var listener: Listener? = null

    fun start() {
        scope.cancel()
        resumeTime = MAX_COUNT
        scope.launch {
            while (resumeTime > 0) {
                listener?.resumeTimeChange(resumeTime)
                resumeTime -= 1
                delay(1000)
            }
            listener?.complete()
        }
    }
}