package com.shinonometn.template.live.server

class ResolveContext(val requestPath : String) {
    val extensionName : String
    val pathWithoutExtension : String

    init {
        val matchResult = extMatcher.find(requestPath)?.groupValues ?: emptyList()
        extensionName = matchResult.lastOrNull()?.drop(1) ?: ""
        pathWithoutExtension = if(extensionName.isBlank()) requestPath else matchResult.getOrNull(1) ?: ""
    }

    fun hasNoExtensionName(): Boolean = extensionName.isBlank()

    companion object {
        private val extMatcher = Regex("^(.+)(\\.[^/]+)$")
    }
}