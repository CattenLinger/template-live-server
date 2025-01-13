package com.shinonometn.template.live.server.routing

import com.shinonometn.template.live.server.scripting.ScriptExecuteState
import com.shinonometn.template.live.server.scripting.ServerScriptBase
import com.shinonometn.template.live.server.scripting.ServerScriptEngine
import groovy.lang.Binding
import io.ktor.server.application.*
import kotlinx.coroutines.supervisorScope
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class ScriptResolvedTarget(urlPath: String) : ResolvedTarget(urlPath) {

    override suspend fun handleApplicationCall(call: ApplicationCall, context: ResolveContext) {
        val scriptEngine = context.server.scriptEngine ?: error("ScriptEngine is not enabled.")
        val env = Binding()

        val scriptName = "+${context.urlPath}"
        env.setProperty("__ScriptName__", scriptName)
        val scriptInstance = scriptEngine.getScriptInstanceDeferred(normalizedPath, env).await()

        supervisorScope {
            val scriptState = ScriptExecuteState(this, scriptInstance, call)
            scriptState.initialize()

            suspendCoroutine {
                CompletableFuture.runAsync({
                    try {
                        scriptInstance.run()
                        it.resume(Unit)
                    } catch (e: Exception) {
                        it.resumeWithException(e)
                    } finally {
                        scriptState.close()
                    }
                }, scriptEngine.executor)
            }
        }
    }
}