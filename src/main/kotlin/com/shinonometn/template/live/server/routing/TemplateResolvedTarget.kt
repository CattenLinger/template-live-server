package com.shinonometn.template.live.server.routing

import io.ktor.server.application.*
import io.ktor.server.response.*

class TemplateResolvedTarget(urlPath: String) : ResolvedTarget(urlPath) {
    override suspend fun handleApplicationCall(call: ApplicationCall, context: ResolveContext) {
        val logger = context.logger
        val engine = context.server.engine

        logger.info("Respond Template '{}'", normalizedPath)

        val requestInfo = call.toRequestInfoMap()

        call.respond(
            engine.provideTemplateContent(
                normalizedPath, mapOf("_request" to requestInfo)
            )
        )
    }
}