package com.shinonometn.template.live.server

import freemarker.cache.FileTemplateLoader
import freemarker.cache.NullCacheStorage
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.thymeleaf.*
import io.ktor.server.velocity.*
import org.apache.velocity.runtime.RuntimeConstants
import org.slf4j.LoggerFactory
import org.thymeleaf.templateresolver.FileTemplateResolver

sealed interface ServerTemplateEngine {

    companion object {
        private val logger = LoggerFactory.getLogger(ServerTemplateEngine::class.java)!!
        val registry = listOf(Freemarker, Velocity, Thymeleaf).associateBy { it.name }
    }

    fun Application.configureServer(serverProfile: ServerProfile)

    fun provideTemplateContent(template : String, model : Map<String, Any>) : Any

    val extensionNames : List<String>

    val name : String

    data object Freemarker : ServerTemplateEngine {
        override val name: String = "freemarker"

        override val extensionNames = listOf("ftl")

        override fun Application.configureServer(serverProfile: ServerProfile) {
            logger.info("Use Apache FreeMarker template engine.")

            install(FreeMarker) {
                templateLoader = FileTemplateLoader(serverProfile.root.toFile())
                cacheStorage = NullCacheStorage()
            }
        }

        override fun provideTemplateContent(template: String, model: Map<String, Any>) =
            FreeMarkerContent(template, model)
    }

    data object Velocity : ServerTemplateEngine {
        override val name: String = "velocity"

        override val extensionNames = listOf("vm")

        override fun Application.configureServer(serverProfile: ServerProfile) {
            logger.info("Use Apache Velocity template engine.")

            install(io.ktor.server.velocity.Velocity) {

                val templateLocation = serverProfile.root.toAbsolutePath().toString()
                    .let { if(it.endsWith("/")) it else "$it/" }

                setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, templateLocation)
                setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "false")
                setProperty(RuntimeConstants.RESOURCE_LOADERS, "file")
            }
        }

        override fun provideTemplateContent(template: String, model: Map<String, Any>) =
            VelocityContent(template, model)
    }

    data object Thymeleaf : ServerTemplateEngine {
        override val name: String = "thymeleaf"

        override val extensionNames = listOf("html")

        override fun Application.configureServer(serverProfile: ServerProfile) {
            logger.info("Use Thymeleaf template engine.")

            install(io.ktor.server.thymeleaf.Thymeleaf) {
                val resolver = FileTemplateResolver()

                resolver.prefix = serverProfile.root.toAbsolutePath().toString()
                    .let { if(it.endsWith("/")) it else "$it/" }

                resolver.characterEncoding = "UTF-8"

                setTemplateResolver(resolver)
            }
        }

        override fun provideTemplateContent(template: String, model: Map<String, Any>) =
            ThymeleafContent(template, model)
    }
}