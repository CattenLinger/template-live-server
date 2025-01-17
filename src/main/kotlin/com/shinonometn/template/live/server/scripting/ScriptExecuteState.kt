package com.shinonometn.template.live.server.scripting

import com.fasterxml.jackson.databind.JsonNode
import com.shinonometn.template.live.server.jsonObjectMapper
import com.shinonometn.template.live.server.handler.EndpointRequestDelegateImpl
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.util.concurrent.CompletableFuture

class ScriptExecuteState(
    val coroutineScope: CoroutineScope,
    val script: ServerScriptBase,
    val call : ApplicationCall
) : Closeable {
    companion object {
        private val log = LoggerFactory.getLogger(ScriptExecuteState::class.java)
    }

    override fun close() {
        output.close()
    }

    fun initialize() {
        val env = script.binding
        env.setVariable("__Request__", EndpointRequestDelegateImpl(call))
        env.setVariable("__Response__", createScriptResponseDelegate())
        env.setVariable("log", log)
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