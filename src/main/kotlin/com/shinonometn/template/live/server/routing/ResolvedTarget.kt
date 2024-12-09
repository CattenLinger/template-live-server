package com.shinonometn.template.live.server.routing

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.*
import java.nio.file.Path

sealed class ResolvedTarget(val urlPath: String) {

    /** Target resource path, resolved to relative file path to the system */
    val normalizedPath: String by lazy { virtualRoot.resolve(urlPath).normalize().toString().drop(1) }

    abstract suspend fun handleApplicationCall(call: ApplicationCall, context: ResolveContext)

    protected fun ApplicationCall.toRequestInfoMap() = mapOf(
        "path" to urlPath,
        "parameters" to parameters.toMap(),
        "httpMethod" to request.httpMethod.value,
        "httpHeaders" to request.headers.toMap(),
    )

    companion object {
        private val virtualRoot = Path.of("/")
    }
}