package com.shinonometn.template.live.server

import com.shinonometn.template.live.server.routing.*
import com.shinonometn.template.live.server.scripting.ServerScriptContext
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.util.*
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.system.exitProcess

class TemplateLiveServer(
    val root: Path,
    val engine: ServerTemplateEngine,
    val extensionNameOverrides: List<String>,
    enableScripting: Boolean
) {
    val scriptEngine: ServerScriptContext? = if (enableScripting) ServerScriptContext(root) else null

    val isScriptEnabled: Boolean
        get() = scriptEngine != null


    //
    // Extension Name resolvers
    //
    class ExtResolver(val ext: String, val targetProvider : suspend ResolveContext.(String) -> ResolvedTarget)

    val extensionNameResolvers: List<ExtResolver>

    init {
        val resolvers = LinkedList<ExtResolver>()
        engine.extensionNames.forEach { ext ->
            resolvers.push(ExtResolver(ext) { TemplateResolvedTarget("${it}.${ext}") })
        }
        if (isScriptEnabled) resolvers.push(
            ExtResolver("groovy") { ScriptResolvedTarget("${it}.groovy") }
        )
        extensionNameResolvers = resolvers
    }
}

private val TemplateLiveServerAttributeKey = AttributeKey<TemplateLiveServer>("TemplateLiveServer")

val Application.serverContext: TemplateLiveServer
    get() = attributes[TemplateLiveServerAttributeKey]

private val logger = LoggerFactory.getLogger("Main")

fun main(args: Array<String>) {
    val profile = ServerProfile.fromArgs(args)

    val serverContext = try {
        TemplateLiveServer(
            root = profile.root.takeIf { it.exists() && it.isDirectory() }
                ?: throw IllegalArgumentException("Template directory does not exist or is not a directory"),

            engine = ServerTemplateEngine.registry[profile.engine.lowercase()]
                ?: throw IllegalArgumentException("Unknown engine"),

            extensionNameOverrides = profile.extensionNameOverwrite,

            enableScripting = profile.isScriptEnabled
        )
    } catch (e: Exception) {
        ServerProfile.printHelp()
        exitProcess(1)
    }

    logger.info("Serve on directory: '{}'", serverContext.root)

    if (serverContext.extensionNameOverrides.isNotEmpty())
        logger.info("Override those extensions to template: {}", serverContext.extensionNameOverrides)

    val server = embeddedServer(Netty, profile.port) {
        attributes.put(TemplateLiveServerAttributeKey, serverContext)

        install(ContentNegotiation) {
            jackson { }
        }

        install(StatusPages) {
            status(HttpStatusCode.NotFound) { call, code ->
                call.respondText(status = code) { "" }
            }
        }

        with(serverContext.engine) {
            configureServer(profile)
        }

        installServerRouting()
    }

    server.start(wait = true)
}