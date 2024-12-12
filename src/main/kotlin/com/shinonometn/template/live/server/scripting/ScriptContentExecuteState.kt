package com.shinonometn.template.live.server.scripting

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.PrintWriter
import java.util.concurrent.CompletableFuture

class ScriptContentExecuteState(
    coroutineScope: CoroutineScope,
    script: ServerScriptBase,
    private val call : ApplicationCall
) : Closeable {
    private val log = script.log

    var contentType :ContentType? = null
        private set

    var statusCode = HttpStatusCode.OK
        private set

    var isWriterExists = false
        private set

    private val scriptJob = Job(coroutineScope.coroutineContext[Job])

    private var outputJob : CompletableJob? = null

    val writer by lazy {
        isWriterExists = true
        val receiver = CompletableFuture<PrintWriter>()
        val outputJob = Job(coroutineScope.coroutineContext[Job])
        coroutineScope.launch {
            call.respondTextWriter(contentType = contentType ?: ContentType.Text.Plain, status = statusCode) {
                receiver.complete(PrintWriter(this, true))
                outputJob.join()
            }
        }
        this.outputJob = outputJob
        receiver.get()
    }

    fun setContentType(str: String?) {
        if(writerExists()) return
        if(str != null) return try {
            contentType = ContentType.parse(str)
        } catch (e : Exception) {
            log.warn("Fail to change content type to {}.", str, e)
        }
        contentType = ContentType.Text.Plain
    }

    fun setStatusCode(code: Int?) {
        if(writerExists()) return
        code ?: return
        try {
            statusCode = HttpStatusCode.fromValue(code)
        } catch (e : Exception) {
            log.warn("Fail to change status to {}.", code, e)
        }
    }

    private fun writerExists() : Boolean {
        if(isWriterExists) log.warn("Writer was opened, any change to http response header will takes no effect.")
        return isWriterExists
    }

    override fun close() {
        if(isWriterExists) writer.flush()
        outputJob?.complete()
    }

    fun createResponseDelegate() = object : ResponseConfigurationDelegate {
        override fun contentType(str: String?) = setContentType(str)
        override fun status(code: Int?) = setStatusCode(code)
        override fun getWriter(): PrintWriter = this@ScriptContentExecuteState.writer
    }
}