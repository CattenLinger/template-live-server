package com.shinonometn.template.live.server.handler

import com.fasterxml.jackson.databind.JsonNode
import groovy.lang.GroovyObjectSupport
import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

class RequestJsonPayload internal constructor(val json: JsonNode) : GroovyObjectSupport(),
    RequestInputDelegate.Payload {
    override val type: String = "json"

    override fun getProperty(propertyName: String?): Any {
        return super.getProperty(propertyName) ?: json.path(propertyName)
    }

    override fun toString(): String {
        return json.toString()
    }

    companion object : RequestInputDelegate.PayloadFactory<RequestJsonPayload> {
        override fun CoroutineScope.create(call: ApplicationCall): RequestJsonPayload {
            val future = CompletableFuture<RequestJsonPayload>()
            launch(Dispatchers.IO) {
                try {
                    val json = call.receive<JsonNode>()
                    future.complete(RequestJsonPayload(json))
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                }
            }
            return future.get()
        }
    }
}