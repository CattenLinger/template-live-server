package com.shinonometn.template.live.server.routing

import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.coroutines.CoroutineScope

class TemplateResolvedTarget(urlPath: String) : ResolvedTarget(urlPath) {
    override suspend fun CoroutineScope.handleApplicationCall(call: ApplicationCall, context: ResolveContext) {
        val logger = context.logger
        val engine = context.server.engine

        logger.info("Respond Template '{}'", normalizedPath)

        val content = engine.provideTemplateContent(this, normalizedPath, context)

        call.respond(content)
    }
}