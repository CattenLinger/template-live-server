package com.shinonometn.template.live.server.scripting.internal

import com.fasterxml.jackson.databind.JsonNode
import com.shinonometn.template.live.server.jsonObjectMapper
import com.shinonometn.template.live.server.scripting.ScriptResponseDelegate
import io.ktor.http.*
import io.ktor.server.application.*
import org.slf4j.Logger
import java.io.InputStream

class ScriptResponseOutputDelegateImpl(
    private val log : Logger,
    private val output : RequestOutputState,
    private val call: ApplicationCall
) : ScriptResponseDelegate {

    override fun contentType(str: String?) {
        try {
            output.contentType = ContentType.parse(str ?: return)
        } catch (e : Exception) {
            log.warn("Fail to change content type to {}.", str, e)
        }
    }

    override fun statusCode(code: Int?) {
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
}