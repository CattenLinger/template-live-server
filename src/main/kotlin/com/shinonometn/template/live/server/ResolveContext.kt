package com.shinonometn.template.live.server

import org.slf4j.Logger

class ResolveContext(val urlPath: String, val logger: Logger) {
    val extensionName: String
    val pathWithoutExtension: String

    init {
        val matchResult = extMatcher.find(urlPath)?.groupValues ?: emptyList()
        extensionName = matchResult.lastOrNull()?.drop(1) ?: ""
        pathWithoutExtension = if (extensionName.isBlank()) urlPath else matchResult.getOrNull(1) ?: ""
    }

    fun hasNoExtensionName(): Boolean = extensionName.isBlank()

    companion object {
        private val extMatcher = Regex("^(.+)(\\.[^/]+)$")

    }
}