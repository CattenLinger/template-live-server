package com.shinonometn.template.live.server.routing

import com.shinonometn.template.live.server.scripting.internal.ScriptExecuteState
import groovy.lang.Binding
import io.ktor.server.application.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.supervisorScope
import java.util.concurrent.CompletableFuture


class ScriptResolvedTarget(urlPath: String) : ResolvedTarget(urlPath) {

    override suspend fun handleApplicationCall(call: ApplicationCall, context: ResolveContext) {
        val scriptEngine = context.server.scriptEngine ?: error("ScriptEngine is not enabled.")
        val env = Binding()
        env.setProperty("__ScriptName__", context.urlPath)

        val script = scriptEngine.getScriptInstanceDeferred(normalizedPath, env).await()

        supervisorScope {
            val scriptState = ScriptExecuteState(this, script, call)
            scriptState.initialize()

            CompletableFuture.runAsync({
                scriptState.use { script.run() }
            }, scriptEngine.executor).await()
        }
    }
}