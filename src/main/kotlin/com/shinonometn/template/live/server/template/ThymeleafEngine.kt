package com.shinonometn.template.live.server.template

import com.shinonometn.template.live.server.ServerProfile
import com.shinonometn.template.live.server.routing.ResolveContext
import io.ktor.server.application.*
import io.ktor.server.thymeleaf.*
import org.thymeleaf.templateresolver.FileTemplateResolver

data object ThymeleafEngine : ServerTemplateEngine {
    override val name: String = "thymeleaf"

    override val extensionNames = listOf("html")

    override fun Application.configureServer(serverProfile: ServerProfile) {
        ServerTemplateEngine.logger.info("Use Thymeleaf template engine.")

        install(Thymeleaf) {
            val resolver = FileTemplateResolver()

            resolver.prefix = serverProfile.root.toAbsolutePath().toString()
                .let { if(it.endsWith("/")) it else "$it/" }

            resolver.characterEncoding = "UTF-8"

            setTemplateResolver(resolver)
        }
    }

    override fun provideTemplateContent(template: String, context: ResolveContext): Any {
        return ThymeleafContent(template, mapOf("request" to context.call.toRequestInfoMap()))
    }
}