package com.shinonometn.template.live.server.template

import com.shinonometn.template.live.server.ServerProfile
import com.shinonometn.template.live.server.routing.ResolveContext
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory

sealed interface ServerTemplateEngine {

    companion object {
        internal val logger = LoggerFactory.getLogger(ServerTemplateEngine::class.java)!!
        val registry = listOf(FreemarkerEngine, VelocityEngine, ThymeleafEngine).associateBy { it.name }
    }

    fun Application.configureServer(serverProfile: ServerProfile)

    fun provideTemplateContent(coroutineScope: CoroutineScope, template : String, context : ResolveContext) : Any

    /** Template file extension names of this engine */
    val extensionNames : List<String>

    /** Name of this engine in string, should be unique */
    val name : String

}