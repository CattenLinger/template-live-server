package com.shinonometn.template.live.server.routing

import com.shinonometn.template.live.server.scripting.ScriptContentExecuteState
import com.shinonometn.template.live.server.scripting.ServerScriptBase
import com.shinonometn.template.live.server.scripting.ServerScriptContext
import groovy.lang.Binding
import io.ktor.server.application.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture


class ScriptResolvedTarget(urlPath: String) : ResolvedTarget(urlPath) {

    private suspend fun <T> ServerScriptContext.executeOnScript(
        script : ServerScriptBase,
        body : ServerScriptBase.() -> T
    ) : T = CompletableFuture.supplyAsync({ body(script) }, executor).await()

    override suspend fun handleApplicationCall(call: ApplicationCall, context: ResolveContext) {
        val scriptEngine = context.server.scriptEngine ?: error("ScriptEngine is not enabled.")
        val env = Binding()

        coroutineScope {
            val scriptInstance = scriptEngine.getScriptInstance(normalizedPath, env).await()

            ScriptContentExecuteState(this, scriptInstance, call).use {
                env.setProperty("request", call.toRequestInfoMap())
                env.setProperty("response", it.createResponseDelegate())
                scriptEngine.executeOnScript(scriptInstance) { run() }
            }
        }
    }
}