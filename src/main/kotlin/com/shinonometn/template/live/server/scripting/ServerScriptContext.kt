package com.shinonometn.template.live.server.scripting

import groovy.lang.Binding
import groovy.util.GroovyScriptEngine
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asDeferred
import java.io.Closeable
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool

class ServerScriptContext(
    scriptRoot: Path,
    private val executorService: ExecutorService = ForkJoinPool.commonPool()
) : Closeable {

    val executor: Executor = executorService

    val engine: GroovyScriptEngine

    init {
        engine = GroovyScriptEngine(
            arrayOf(scriptRoot.toAbsolutePath().toUri().toURL())
        )

        val config = engine.config
        config.scriptBaseClass = ServerScriptBase::class.java.name
        config.scriptExtensions = setOf(".groovy")
    }

    fun getScriptInstance(script: String, binding: Binding) : Deferred<ServerScriptBase> {
        return CompletableFuture.supplyAsync({
            engine.createScript(script, binding) as ServerScriptBase
        }, executor).asDeferred()
    }

    override fun close() {
        executorService.shutdown()
    }
}