package com.shinonometn.template.live.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.freemarker.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

private val virtualRoot = Path.of("/")

private val resolveLog = LoggerFactory.getLogger("PathResolver")

private fun ServerTemplateEngine.resolveTemplateNamesFor(path : String) = extensionNames.map {
    ResolvedTarget.Template("$path.$it")
}

fun Application.installServerRouting() {
    val server = serverContext
    val engine = server.engine

    intercept(ApplicationCallPipeline.Call) {
        val ctx = ResolveContext(call.request.path())
        val params = call.request.queryParameters

        val targets = mutableListOf<ResolvedTarget>()

        if(!ctx.requestPath.endsWith('/')) when {
            // If request has no extension name, use template first
            ctx.hasNoExtensionName() -> targets.addAll(
                engine.resolveTemplateNamesFor(ctx.pathWithoutExtension)
            )

            else -> when (ctx.extensionName) {
                // If extension name is template extension, use template first
                in engine.extensionNames -> targets.add(
                    ResolvedTarget.Template(ctx.requestPath)
                )

                // If the extension name is override by settings, use template
                in server.extensionNameOverrides -> targets.addAll(
                    engine.resolveTemplateNamesFor(ctx.pathWithoutExtension)
                )
            }
        }

        // The file fallback
        targets.add(ResolvedTarget.File(ctx.requestPath))

        resolveLog.info("Request URI: '{}'.", call.request.uri)

        // Resolve on each target
        suspend fun resolve(resolveList : List<ResolvedTarget>) : Boolean {
            resolveLog.info("Resolve {} targets.", resolveList.size)
            for(rt in resolveList) {
                val normalizedPath = virtualRoot.resolve(rt.pathString).normalize().toString().drop(1)
                val file = server.root.resolve(normalizedPath)
                resolveLog.info("Try to resolve '{}'.", file)
                if(!file.exists()) continue

                if(file.isDirectory()) {
                    resolveLog.info("'{}' is a directory, try to resolve recursively.", normalizedPath)

                    val list = mutableListOf<ResolvedTarget>()
                    list.addAll(engine.extensionNames.map {
                        ResolvedTarget.Template("$normalizedPath/index.$it")
                    })
                    list.add(ResolvedTarget.File("$normalizedPath/index.html"))
                    list.add(ResolvedTarget.File("$normalizedPath/index.htm"))
                    return resolve(list)
                }

                when(rt) {
                    is ResolvedTarget.File -> {
                        resolveLog.info("Respond File '{}'", file)
                        call.respondPath(file)
                    }
                    is ResolvedTarget.Template -> {
                        resolveLog.info("Respond Template '{}'", normalizedPath)
                        call.respondTemplate(normalizedPath, mapOf("params" to params))
                    }
                }
                return true
            }
            return false
        }

        if(!resolve(targets)) {
            resolveLog.info("Path '{}' not found.", ctx.requestPath)
            call.respond(HttpStatusCode.NotFound) { "Not found" }
        }
    }
}