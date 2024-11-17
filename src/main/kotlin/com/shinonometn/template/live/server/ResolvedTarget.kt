package com.shinonometn.template.live.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import java.nio.file.Path

sealed class ResolvedTarget(val urlPath: String) {

    val normalizedPath: String by lazy { virtualRoot.resolve(urlPath).normalize().toString().drop(1) }

    abstract suspend fun handleApplicationCall(call: ApplicationCall, resolveContext: ResolveContext)

    class Template(urlPath: String) : ResolvedTarget(urlPath) {
        override suspend fun handleApplicationCall(call: ApplicationCall, resolveContext: ResolveContext) {
            val logger = resolveContext.logger
            logger.info("Respond Template '{}'", normalizedPath)

            val requestInfo = mapOf(
                "path" to urlPath,
                "parameters" to call.parameters.toMap(),
                "httpMethod" to call.request.httpMethod.value,
                "httpHeaders" to call.request.headers.toMap(),
            )

            call.respond(
                call.application.serverContext.engine.provideTemplateContent(
                    normalizedPath, mapOf("_request" to requestInfo)
                )
            )
        }
    }

    class File(urlPath: String) : ResolvedTarget(urlPath) {
        override suspend fun handleApplicationCall(call: ApplicationCall, resolveContext: ResolveContext) {
            if (call.request.httpMethod != HttpMethod.Get) return call.respond(HttpStatusCode.MethodNotAllowed)

            val path = call.application.serverContext.root.resolve(normalizedPath)

            resolveContext.logger.info("Respond File '{}'", path)
            call.respondPath(path)
        }
    }

    companion object {
        private val virtualRoot = Path.of("/")
    }
}