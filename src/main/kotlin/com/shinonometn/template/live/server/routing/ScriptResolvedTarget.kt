package com.shinonometn.template.live.server.routing

import com.shinonometn.template.live.server.scripting.ResponseConfigurationDelegate
import com.shinonometn.template.live.server.scripting.ServerScriptBase
import com.shinonometn.template.live.server.scripting.ServerScriptContext
import groovy.lang.Binding
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import java.io.Closeable
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture

private class ScriptContentExecuteState(
    coroutineScope: CoroutineScope,
    script: ServerScriptBase,
    private val call : ApplicationCall
) : Closeable {
    private val log = script.log

    var contentType = ContentType.Text.Plain
        private set

    var statusCode = HttpStatusCode.OK
        private set

    var isWriterExists = false
        private set

    private var outputJob : CompletableJob? = null

    val writer by lazy {
        isWriterExists = true
        val receiver = CompletableFuture<PrintWriter>()
        val outputJob = Job(coroutineScope.coroutineContext[Job])
        coroutineScope.launch {
            call.respondTextWriter(contentType = contentType, status = statusCode) {
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

    fun createResponseDelegate() = object :
        ResponseConfigurationDelegate {
        override fun contentType(str: String?) = setContentType(str)
        override fun status(code: Int?) = setStatusCode(code)
        override fun getWriter(): PrintWriter = this@ScriptContentExecuteState.writer
    }
}


class ScriptResolvedTarget(urlPath: String) : ResolvedTarget(urlPath) {

    private suspend fun <T> ServerScriptContext.executeOnScript(
        script : ServerScriptBase,
        body : ServerScriptBase.() -> T
    ) : T = supervisorScope { CompletableFuture.supplyAsync({ body(script) }, executor).await() }

    override suspend fun handleApplicationCall(call: ApplicationCall, context: ResolveContext) {
        val scriptEngine = context.server.scriptEngine ?: error("ScriptEngine is not enabled.")

        val env = Binding()
        val scriptInstance = scriptEngine.getScriptInstance(normalizedPath, env).await()

        supervisorScope {
            ScriptContentExecuteState(this, scriptInstance, call).use {
                env.setProperty("request", call.toRequestInfoMap())
                env.setProperty("response", it.createResponseDelegate())
                scriptEngine.executeOnScript(scriptInstance) { run() }
            }
        }
    }
}