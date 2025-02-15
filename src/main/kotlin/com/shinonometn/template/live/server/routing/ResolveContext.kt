package com.shinonometn.template.live.server.routing

import com.shinonometn.template.live.server.TemplateLiveServer
import com.shinonometn.template.live.server.serverContext
import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.coroutines.CoroutineScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("ResolveContext")

fun TemplateLiveServer.newResolveContext(call: ApplicationCall, urlPath: String) =
    ResolveContext(call, urlPath, logger)

class ResolveContext internal constructor(
    val call: ApplicationCall,
    val urlPath: String,
    val logger: Logger
) {
    val server = call.application.serverContext

    val coroutineScope : CoroutineScope = call

    /**
     * Url Extension Name.
     *
     * Empty if path does not include an extension name.
     */
    val extensionName: String

    /**
     * Url path without extension name.
     *
     * Same as [urlPath] if path does not include an extension name
     */
    val pathWithoutExtension: String

    init {
        val matchResult = extMatcher.find(urlPath)?.groupValues ?: emptyList()
        extensionName = matchResult.lastOrNull()?.drop(1) ?: ""
        pathWithoutExtension = if (extensionName.isBlank()) urlPath else matchResult.getOrNull(1) ?: ""
    }

    companion object {
        private val extMatcher = Regex("^(.+)(\\.[^/]+)$")
    }
}