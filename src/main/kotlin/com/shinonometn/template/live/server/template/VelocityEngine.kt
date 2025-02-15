package com.shinonometn.template.live.server.template

import com.shinonometn.template.live.server.ServerProfile
import com.shinonometn.template.live.server.handler.EndpointRequestDelegateImpl
import com.shinonometn.template.live.server.handler.RequestInputDelegate
import com.shinonometn.template.live.server.handler.RequestInputDelegate.Companion.TemplateInputPayloadDecider
import com.shinonometn.template.live.server.routing.ResolveContext
import io.ktor.server.application.*
import io.ktor.server.velocity.*
import io.ktor.util.*
import org.apache.velocity.app.VelocityEngine
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

            // Put engine in context
            attributes.put(VelocityEngineAttributeKey, this)
        }
    }

    override fun provideTemplateContent(template: String, context: ResolveContext): Any {
        val body = RequestInputDelegate(context.call, TemplateInputPayloadDecider)
        return VelocityContent(template, mapOf("request" to EndpointRequestDelegateImpl(context.call) { body.payload }))
    }

    private val VelocityEngineAttributeKey = AttributeKey<VelocityEngine>("VelocityEngine")
    private val Application.velocityEngine: VelocityEngine
        get() = attributes[VelocityEngineAttributeKey]
}