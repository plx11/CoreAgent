package com.coreagent.core.engine

import com.squareup.duktape.Duktape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ExecutionEngine {
    private val executor = Executors.newSingleThreadExecutor()

    suspend fun execute(script: String, timeoutSeconds: Long = 10): String = withContext(Dispatchers.Default) {
        val future = executor.submit<String> {
            val duktape = Duktape.create()
            try {
                duktape.evaluate(script).toString()
            } finally {
                duktape.close()
            }
        }
        
        try {
            future.get(timeoutSeconds, TimeUnit.SECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            throw Exception("腳本執行超時")
        } catch (e: Exception) {
            throw Exception("腳本執行錯誤: ${e.message}")
        }
    }
}
