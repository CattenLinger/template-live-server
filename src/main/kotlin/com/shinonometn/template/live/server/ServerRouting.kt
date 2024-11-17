package com.shinonometn.template.live.server

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

private val resolveLog = LoggerFactory.getLogger("PathResolver")

private fun ServerTemplateEngine.resolveTemplateNamesFor(path: String) = extensionNames.map {
    ResolvedTarget.Template("$path.$it")
}

fun Application.installServerRouting() {
    val server = serverContext
    val engine = server.engine

    intercept(ApplicationCallPipeline.Call) {
        val ctx = ResolveContext(call.request.path(), resolveLog)

        val targets = mutableListOf<ResolvedTarget>()

        if (!ctx.urlPath.endsWith('/')) when {
            // If request has no extension name, use template first
            ctx.hasNoExtensionName() -> targets.addAll(
                engine.resolveTemplateNamesFor(ctx.pathWithoutExtension)
            )

            else -> when (ctx.extensionName) {
                // If extension name is template extension, use template first
                in engine.extensionNames -> targets.add(
                    ResolvedTarget.Template(ctx.urlPath)
                )

                // If the extension name is override by settings, use template
                in server.extensionNameOverrides -> targets.addAll(
                    engine.resolveTemplateNamesFor(ctx.pathWithoutExtension)
                )
            }
        }

        // The file fallback
        targets.add(ResolvedTarget.File(ctx.urlPath))

        resolveLog.info("Request URI: '{}'.", call.request.uri)

        // Resolve on each target
        suspend fun resolve(resolveList: List<ResolvedTarget>): Boolean {
            resolveLog.info("Resolve {} targets.", resolveList.size)
            for (rt in resolveList) {
                val normalizedPath = rt.normalizedPath

                val file = server.root.resolve(normalizedPath)
                resolveLog.info("Try to resolve '{}'.", file)
                if (!file.exists()) continue

                if (file.isDirectory()) {
                    resolveLog.info("'{}' is a directory, try to resolve recursively.", normalizedPath)

                    val list = mutableListOf<ResolvedTarget>()
                    list.addAll(engine.extensionNames.map {
                        ResolvedTarget.Template("$normalizedPath/index.$it")
                    })
                    list.add(ResolvedTarget.File("$normalizedPath/index.html"))
                    list.add(ResolvedTarget.File("$normalizedPath/index.htm"))
                    return resolve(list)
                }

                rt.handleApplicationCall(call, ctx)

                return true
            }
            return false
        }

        if (!resolve(targets)) {
            resolveLog.info("Path '{}' not found.", ctx.urlPath)
            call.respond(HttpStatusCode.NotFound) { "Not found" }
        }
    }
}