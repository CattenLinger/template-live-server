package com.shinonometn.template.live.server.scripting

import com.shinonometn.template.live.server.TemplateLiveServer
import groovy.lang.Binding
import groovy.lang.GroovyClassLoader
import groovy.lang.GroovyObjectSupport
import groovy.lang.GroovyShell
import groovy.lang.Script
import groovy.transform.ThreadInterrupt
import groovy.util.GroovyScriptEngine
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asDeferred
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.ForkJoinPool
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

class ServerScriptEngine(
    scriptRoot: Path,
    private val executorService: ExecutorService = ForkJoinPool.commonPool()
) : Closeable {

    val log = LoggerFactory.getLogger("ServerScriptEngine")

    val executor: Executor = executorService

    val scriptRoot = scriptRoot.toAbsolutePath()
    val scriptPrivateRoot = scriptRoot.resolve("WEB-INF")

    internal val engine: GroovyScriptEngine

    internal val shell : GroovyShell
    private val binding : Binding

    init {

        engine = GroovyScriptEngine(arrayOf(scriptRoot.toUri().toURL()))

        val config = engine.config
        config.scriptBaseClass = ServerScriptBase::class.java.name
        config.scriptExtensions = setOf(".groovy")
        config.sourceEncoding = StandardCharsets.UTF_8.name()
        // https://www.groovy-lang.org/metaprogramming.html#_safer_scripting
        config.addCompilationCustomizers(ASTTransformationCustomizer(ThreadInterrupt::class.java))

        binding = Binding()
        shell = GroovyShell(binding)
    }

    internal fun initServerContext(server : TemplateLiveServer) {
        binding.setVariable("instance", server)
        val configScriptPath = scriptPrivateRoot.resolve("server.config.groovy")
        if(configScriptPath.exists()) {
            shell.evaluate(configScriptPath.toAbsolutePath().toFile())
            log.info("Initialized server script from '$configScriptPath'.")
        }
    }

    inner class ScriptDelegate internal constructor() : GroovyObjectSupport() {
        fun propertyMissing(key: String) : Any? {
            return binding.getProperty(key)
        }
    }
    val scriptDelegate = ScriptDelegate()

    fun getScriptInstanceDeferred(name: String, binding: Binding) : Deferred<ServerScriptBase> {
        return CompletableFuture.supplyAsync({
            engine.createScript(name, binding) as ServerScriptBase
        }, executor).asDeferred()
    }

    override fun close() {
        executorService.shutdown()
    }

    companion object {
        val virtualRoot = Path.of("/")
        val privateRoot = Path.of("/WEB-INF")
    }
}