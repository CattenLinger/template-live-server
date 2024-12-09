package com.shinonometn.template.live.server.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

class FileResolvedTarget(urlPath: String) : ResolvedTarget(urlPath) {
    override suspend fun handleApplicationCall(call: ApplicationCall, context: ResolveContext) {
        val logger = context.logger

        // Returns NOT_ALLOWED when the file request method is not get
        if (call.request.httpMethod != HttpMethod.Get)
            return call.respond(HttpStatusCode.MethodNotAllowed)

        val path = context.server.root.resolve(normalizedPath)
        logger.info("Respond File '{}'", path)

        call.respondPath(path)
    }
}