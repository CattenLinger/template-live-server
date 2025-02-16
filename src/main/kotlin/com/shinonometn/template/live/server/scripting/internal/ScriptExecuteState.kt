package com.shinonometn.template.live.server.scripting.internal

import com.shinonometn.template.live.server.handler.EndpointRequestDelegateImpl
import com.shinonometn.template.live.server.handler.RequestInputDelegate
import com.shinonometn.template.live.server.scripting.ServerScriptBase
import com.shinonometn.template.live.server.serverContext
import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory
import java.io.Closeable

class ScriptExecuteState(
    val coroutineScope: CoroutineScope,
    val script: ServerScriptBase,
    val call : ApplicationCall
) : Closeable {
    companion object {
        private val log = LoggerFactory.getLogger(ScriptExecuteState::class.java)
    }

    override fun close() {
        input.close()
        output.close()
    }

    fun initialize() {
        val env = script.binding

        env.setVariable("request", EndpointRequestDelegateImpl(call) { input.payload })
        env.setVariable("response", ScriptResponseOutputDelegateImpl(log, output, call))
        env.setVariable("server", call.application.serverContext.scriptEngine?.scriptDelegate)
        env.setVariable("log", log)
    }

    val input = RequestInputDelegate(coroutineScope, call)

    val output = RequestOutputState(coroutineScope, call, log)

}