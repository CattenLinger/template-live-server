package com.shinonometn.template.live.server.handler

import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

class RequestStringPayload internal constructor(val text: String) : RequestInputDelegate.Payload {
    override val type: String = "text"

    override fun toString(): String {
        return text
    }

    companion object : RequestInputDelegate.PayloadFactory<RequestStringPayload> {
        override fun CoroutineScope.create(call: ApplicationCall): RequestStringPayload {
            val f = CompletableFuture<RequestStringPayload>()
            launch(Dispatchers.IO) {
                try {
                    val text = call.receiveText()
                    f.complete(RequestStringPayload(text))
                } catch (e: Exception) {
                    f.completeExceptionally(e)
                }
            }
            return f.get()
        }
    }
}