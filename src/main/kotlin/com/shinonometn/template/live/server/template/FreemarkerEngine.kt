package com.shinonometn.template.live.server.template

import com.shinonometn.template.live.server.ServerProfile
import com.shinonometn.template.live.server.handler.EndpointRequestDelegateImpl
import com.shinonometn.template.live.server.handler.RequestInputDelegate
import com.shinonometn.template.live.server.handler.RequestInputDelegate.Companion.TemplateInputPayloadDecider
import com.shinonometn.template.live.server.routing.ResolveContext
import freemarker.cache.FileTemplateLoader
import freemarker.cache.NullCacheStorage
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import kotlinx.coroutines.CoroutineScope

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

    override fun provideTemplateContent(coroutineScope: CoroutineScope, template: String, context : ResolveContext): FreeMarkerContent {
        val body = RequestInputDelegate(coroutineScope, context.call, TemplateInputPayloadDecider)
        return FreeMarkerContent(
            template,
            mapOf("request" to EndpointRequestDelegateImpl(context.call) { body.payload })
        )
    }
}