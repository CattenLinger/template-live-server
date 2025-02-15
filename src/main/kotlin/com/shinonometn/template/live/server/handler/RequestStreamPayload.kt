package com.shinonometn.template.live.server.handler

import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.concurrent.CompletableFuture

class RequestStreamPayload internal constructor(val stream: InputStream) : RequestInputDelegate.Payload {
    override val type: String = "stream"

    companion object : RequestInputDelegate.PayloadFactory<RequestStreamPayload> {
        override fun CoroutineScope.create(call: ApplicationCall): RequestStreamPayload {
            val future = CompletableFuture<RequestStreamPayload>()
            launch(Dispatchers.IO) {
                try {
                    val stream = call.receiveStream()
                    future.complete(RequestStreamPayload(stream))
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                }
            }
            return future.get()
        }
    }
}