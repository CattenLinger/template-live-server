package com.shinonometn.template.live.server.handler

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.coroutines.CoroutineScope
import java.io.Closeable

class RequestInputDelegate internal constructor(
    val coroutineScope: CoroutineScope,
    val call: ApplicationCall,
    private val decidePayloadFactory: (ctx: RequestInputDelegate) -> PayloadFactory<out Payload> = ScriptInputPayloadDecider
) : Closeable {

    private var hasPayload: Boolean = false

    val payload by lazy { createPayloadDelegate() }

    private fun createPayloadDelegate(): Payload {
        val payload = with(decidePayloadFactory(this)) { coroutineScope.create(call) }
        hasPayload = true
        return payload
    }

    sealed interface Payload {
        val type: String

        data object Empty : Payload {
            override val type: String = "empty"

            data object Factory : PayloadFactory<Empty> {
                override fun CoroutineScope.create(call: ApplicationCall): Empty {
                    return Empty
                }
            }
        }
    }

    sealed interface PayloadFactory<Payload> {
        fun CoroutineScope.create(call: ApplicationCall): Payload
    }

    override fun close() {
        if (!hasPayload) return

        when(val payload = payload) {
            is RequestStreamPayload -> payload.stream.close()
            else -> {}
        }
    }

    companion object {
        val ScriptInputPayloadDecider: (RequestInputDelegate) -> PayloadFactory<out Payload> = {
            val contentType = it.call.request.contentType()
            when {
                "text" == contentType.contentType -> RequestStringPayload
                ContentType.Application.FormUrlEncoded.match(contentType) -> RequestFormPayload
                ContentType.Application.Json.match(contentType) -> RequestJsonPayload
                else -> RequestStreamPayload
            }
        }

        val TemplateInputPayloadDecider : (RequestInputDelegate) -> PayloadFactory<out Payload> = {
            val contentType = it.call.request.contentType()
            when {
                "text" == contentType.contentType -> RequestStringPayload
                ContentType.Application.FormUrlEncoded.match(contentType) -> RequestFormPayload
                ContentType.Application.Json.match(contentType) -> RequestJsonPayload
                else -> Payload.Empty.Factory
            }
        }
    }
}