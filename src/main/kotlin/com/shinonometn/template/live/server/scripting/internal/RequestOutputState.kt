package com.shinonometn.template.live.server.scripting.internal

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.slf4j.Logger
import java.io.OutputStream
import java.io.PrintWriter
import java.util.concurrent.CompletableFuture

/** An object remembers script's response output state */
class RequestOutputState internal constructor(
    private val coroutineScope: CoroutineScope,
    private val call : ApplicationCall, private val log : Logger
) {

    var contentType : ContentType? = null
        set(value) {
            if(writerExists()) return log.warn("Writer was exists, change for contentType has no effect.")
            field = value
        }

    var statusCode = HttpStatusCode.OK
        set(value) {
            if(writerExists()) return log.warn("Writer was exists, change for status code has no effect.")
            field = value
        }

    private var didOutputCreated = false
    private val outputJob by lazy { Job(coroutineScope.coroutineContext[Job]) }
    val outputStream by lazy {
        if(outputJob.isCompleted) throw IllegalStateException("Output job was completed.")
        val receiver = CompletableFuture<OutputStream>()
        coroutineScope.launch {
            call.respondOutputStream(contentType, statusCode) {
                receiver.complete(this)
                outputJob.join()
            }
        }
        receiver.get().also { didOutputCreated = true }
    }
    private var isPrinterExists = false
    val printer by lazy { PrintWriter(outputStream).also { isPrinterExists = true } }

    private fun writerExists() : Boolean {
        if(didOutputCreated) log.warn("Writer was opened, any change to http response header will takes no effect.")
        return didOutputCreated
    }

    fun close() {
        if(didOutputCreated) {
            if(isPrinterExists) printer.flush()

            outputJob.complete()
        }
    }
}