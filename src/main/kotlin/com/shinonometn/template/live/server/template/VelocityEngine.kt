package com.shinonometn.template.live.server.template

import com.shinonometn.template.live.server.ServerProfile
import com.shinonometn.template.live.server.routing.ResolveContext
import io.ktor.server.application.*
import io.ktor.server.velocity.*
import org.apache.velocity.runtime.RuntimeConstants

data object VelocityEngine : ServerTemplateEngine {
    override val name: String = "velocity"

    override val extensionNames = listOf("vm")

    override fun Application.configureServer(serverProfile: ServerProfile) {
        ServerTemplateEngine.logger.info("Use Apache Velocity template engine.")

        install(Velocity) {

            val templateLocation = serverProfile.root.toAbsolutePath().toString()
                .let { if(it.endsWith("/")) it else "$it/" }

            setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, templateLocation)
            setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "false")
            setProperty(RuntimeConstants.RESOURCE_LOADERS, "file")
        }
    }

    override fun provideTemplateContent(template: String, context: ResolveContext): Any {
        return VelocityContent(template, mapOf("request" to context.call.toRequestInfoMap()))
    }
}