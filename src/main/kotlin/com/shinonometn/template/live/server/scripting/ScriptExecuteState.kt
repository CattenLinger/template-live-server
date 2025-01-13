package com.shinonometn.template.live.server.scripting

import com.fasterxml.jackson.databind.JsonNode
import com.shinonometn.template.live.server.jsonObjectMapper
import com.shinonometn.template.live.server.scripting.ServerScriptEngine.Companion.privateRoot
import com.shinonometn.template.live.server.scripting.ServerScriptEngine.Companion.virtualRoot
import com.shinonometn.template.live.server.serverContext
import groovy.lang.Binding
import groovy.lang.Script
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

class ScriptExecuteState(
    internal val coroutineScope: CoroutineScope,
    private val script: ServerScriptBase,
    internal val call : ApplicationCall
) : Closeable {
    private val log = script.log

    override fun close() {
        output.close()
    }

    fun initialize() {
        val env = script.binding
        env.setVariable("__Request__", createScriptRequestDelegate())
        env.setVariable("__Response__", createScriptResponseDelegate())
    }

//    private fun getScriptEngine() = call.application.serverContext.scriptEngine!!

//    private fun createScriptServiceDelegate() = object : ScriptServiceDelegate {
//        override fun getScriptName(): String = script.binding.getVariable("__ScriptName__") as String
//
//        override fun require(scriptName: String): Script {
//            if(scriptName.isBlank()) throw IllegalArgumentException("ScriptName should not be blank.")
//            val scriptLeading = scriptName.first()
//            val scriptLocation = scriptName.drop(1)
//            if(scriptLocation.isBlank()) throw IllegalArgumentException("ScriptName should not be blank.")
//
//            // +/scriptName
//            // /scriptName
//            // -/scriptName
//            // scriptName
//
//            // Script path is related to the server's script root.
//            // Server's script root is GroovyScriptEngine's path
//            val scriptPath = when(scriptLeading) {
//                '+', '/' -> { // Related to public root
//                    val relativePath = scriptName.drop(1)
//                    virtualRoot.resolve(relativePath).normalize()
//                }
//                '-' -> { // Related to private root
//                    val relativePath = scriptName.drop(1)
//                    val virtualPath = virtualRoot.resolve(relativePath).normalize().toString().drop(1)
//                    privateRoot.resolve(virtualPath)
//                }
//                else -> { // Related to current file's root
//                    val scriptParent = Path.of(getScriptName()).parent
//                    val relativePath = scriptName.drop(1)
//                    scriptParent.resolve(relativePath).normalize()
//                }
//            }.toString().drop(1)
//
//            log.debug("Script {} require script {}.", getScriptName(), scriptName)
//            return getScriptEngine().engine.createScript(scriptPath, Binding())
//        }
//    }

    /** Method to create a request access delegate for the script */
    private fun createScriptRequestDelegate() = object : ScriptRequestDelegate {
        override fun getMethod(): String = call.request.httpMethod.value
        override fun getPath(): String = call.request.path()

        private val _url by lazy { call.request.uri.substringBefore("?") }
        override fun getUrl(): String = _url

        override fun getUri(): String = call.request.uri

        override fun parameter(name: String): String? = call.request.queryParameters[name]

        override fun getQueryString(): String = call.request.queryString()

        override fun header(name: String): String? = call.request.header(name)

        override fun cookie(name: String): String? = call.request.cookies[name]

        private val _parameterMap by lazy {
            call.parameters.toMap().mapValues { it.value.toMutableList() }.toMutableMap()
        }
        override fun getParameters(): MutableMap<String, MutableList<String>> = _parameterMap

        private val _headerMap by lazy {
            call.request.headers.toMap().mapValues { it.value.toMutableList() }.toMutableMap()
        }
        override fun getHeaders(): MutableMap<String, MutableList<String>> = _headerMap

        private val _cookieMap by lazy { call.request.cookies.rawCookies.toMutableMap() }
        override fun getCookies(): MutableMap<String, String> = _cookieMap

        private val _contentType by lazy { call.request.contentType().contentType }
        override fun getContentType(): String = _contentType
    }

    /** An object remembers script's response output state */
    inner class OutputState internal constructor() {
        var contentType :ContentType? = null
            set(value) {
                if(writerExists()) return log.warn("Writer was exists, change for contentType has no effect.")
                field = value
            }

        var statusCode = HttpStatusCode.OK
            set(value) {
                if(writerExists()) return log.warn("Writer was exists, change for status code has no effect.")
                field = value
            }

        private val outputJob by lazy { Job(coroutineScope.coroutineContext[Job]) }
        private var didOutputCreated = false
        val outputStream by lazy {
            if(outputJob.isCompleted) throw IllegalStateException("Output job was completed.")
            val receiver = CompletableFuture<OutputStream>()
            coroutineScope.launch {
                call.respondOutputStream(contentType, statusCode) {
                    receiver.complete(this)
                    outputJob.join()
                }
            }
            receiver.get()
        }
        private var isPrinterExists = false
        val printer by lazy {
            val printer = PrintWriter(outputStream)
            isPrinterExists = true
            printer
        }

        private fun writerExists() : Boolean {
            if(didOutputCreated) log.warn("Writer was opened, any change to http response header will takes no effect.")
            return didOutputCreated
        }

        fun close() {
            if(didOutputCreated) {
                if(isPrinterExists) printer.flush()
                outputStream.close()
            }
        }
    }
    val output = OutputState()

    private fun writeObject(value : Any?) : Any? {
        if(value == null) return null
        if(value is InputStream) {
            val copied = value.copyTo(output.outputStream)
            output.outputStream.flush()
            return copied
        }

        val printer = output.printer
        when(value) {
            is JsonNode -> call.application.jsonObjectMapper.writeValue(output.printer, value)
            else -> printer.print(value)
        }
        printer.flush()
        return null
    }

    /** Method to create a response access delegate for the script */
    private fun createScriptResponseDelegate() = object : ScriptResponseDelegate {
        override fun contentType(str: String?) {
            try {
                output.contentType = ContentType.parse(str ?: return)
            } catch (e : Exception) {
                log.warn("Fail to change content type to {}.", str, e)
            }
        }

        override fun status(code: Int?) {
            try {
                output.statusCode = HttpStatusCode.fromValue(code ?: return)
            } catch (e : Exception) {
                log.warn("Fail to change status to {}.", code, e)
            }
        }

        private fun outputString(s : String) {
            val p = output.printer
            p.print(s)
            p.flush()
        }

        override fun leftShift(s: String) = outputString(s)
        override fun call(s: String) = outputString(s)

        override fun leftShift(o: Any?): Any? = writeObject(o)
        override fun call(o: Any?): Any? = writeObject(o)

    }
}