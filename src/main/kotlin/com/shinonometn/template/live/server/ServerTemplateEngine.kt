package com.shinonometn.template.live.server

import freemarker.cache.FileTemplateLoader
import freemarker.cache.NullCacheStorage
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import org.apache.velocity.runtime.RuntimeConstants
import org.slf4j.LoggerFactory

sealed interface ServerTemplateEngine {

    fun Application.configureServer(serverProfile: ServerProfile)

    val extensionNames : List<String>

    val name : String

    companion object {
        private val logger = LoggerFactory.getLogger(ServerTemplateEngine::class.java)!!

        val registry = listOf(Freemarker, Velocity).associateBy { it.name }
    }

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
    }

    data object Velocity : ServerTemplateEngine {
        override val name: String = "velocity"

        override val extensionNames = listOf("vl")

        override fun Application.configureServer(serverProfile: ServerProfile) {
            logger.info("Use Apache Velocity template engine.")

            install(io.ktor.server.velocity.Velocity) {
                setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, serverProfile.root.toAbsolutePath().toString())
                setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "false")
                setProperty(RuntimeConstants.RESOURCE_LOADERS, "file")
            }
        }
    }
}