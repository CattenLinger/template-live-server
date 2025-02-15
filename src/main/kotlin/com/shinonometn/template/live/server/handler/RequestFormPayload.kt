package com.shinonometn.template.live.server.handler

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

class RequestFormPayload internal constructor(private val parameters: Parameters) : RequestInputDelegate.Payload {
    override val type: String = "form"

    fun getAt(key: String): String? = parameters[key]

    fun getAll(key: String): List<String>? = parameters.getAll(key)

    fun getAll(): Map<String, List<String>> = parameters.toMap()

    fun contains(key: String) = parameters.contains(key)

    companion object : RequestInputDelegate.PayloadFactory<RequestFormPayload> {
        override fun CoroutineScope.create(call: ApplicationCall): RequestFormPayload {
            val future = CompletableFuture<RequestFormPayload>()
            launch(Dispatchers.IO) {
                try {
                    val params = call.receiveParameters()
                    future.complete(RequestFormPayload(params))
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                }
            }
            return future.get()
        }
    }
}