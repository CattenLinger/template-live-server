package com.shinonometn.template.live.server.template

import com.shinonometn.template.live.server.ServerProfile
import com.shinonometn.template.live.server.routing.ResolveContext
import freemarker.cache.FileTemplateLoader
import freemarker.cache.NullCacheStorage
import io.ktor.server.application.*
import io.ktor.server.freemarker.*

data object FreemarkerEngine : ServerTemplateEngine {
    override val name: String = "freemarker"
    override val extensionNames = listOf("ftl")

    override fun Application.configureServer(serverProfile: ServerProfile) {
        ServerTemplateEngine.logger.info("Use Apache FreeMarker template engine.")

        install(FreeMarker) {
            templateLoader = FileTemplateLoader(serverProfile.root.toFile())
            cacheStorage = NullCacheStorage()
        }
    }

    override fun provideTemplateContent(template: String, context : ResolveContext): FreeMarkerContent {
        return FreeMarkerContent(template, mapOf("request" to context.call.toRequestInfoMap()))
    }
}