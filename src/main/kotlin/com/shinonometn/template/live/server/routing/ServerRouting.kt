package com.shinonometn.template.live.server.routing

import com.shinonometn.template.live.server.serverContext
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import org.slf4j.LoggerFactory
import java.util.Optional
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

private val resolveLog = LoggerFactory.getLogger("PathResolver")

private suspend fun ResolveContext.resolveSearchPaths(): MutableList<ResolvedTarget> {
    val targets = mutableListOf<ResolvedTarget>()
    val ext = extensionName

    if (!urlPath.endsWith("/")) {
        when {
            // If request has no extension name, use template first
            // If the extension name is override by settings, use template
            ext.isBlank() || (ext in server.extensionNameOverrides) -> targets.addAll(
                server.extensionNameResolvers.map { it.targetProvider(this, pathWithoutExtension) }
            )

            // If extension name is template extension, use template first
            ext in server.engine.extensionNames -> targets.add(
                TemplateResolvedTarget(urlPath)
            )
        }

        // If server script engine enabled and extension name is "groovy"
        if (server.isScriptEnabled && ext == "groovy") targets.add(ScriptResolvedTarget(urlPath))
    }

    // The file fallback
    targets.add(FileResolvedTarget(urlPath))

    return targets
}

private suspend fun ResolveContext.resolveIndexForDirectory(normalizedPath: String): List<ResolvedTarget> {
    val targets = mutableListOf<ResolvedTarget>()

    targets += server.extensionNameResolvers.map {
        it.targetProvider(this, "${normalizedPath}/index")
    }

    val indexFiles = server.indexFiles
    if (indexFiles.isNotEmpty()) targets += indexFiles.map { name ->
        FileResolvedTarget("${normalizedPath}/$name")
    }

    return targets
}

private suspend fun ResolveContext.resolve(targets: List<ResolvedTarget>): ResolvedTarget? {
    resolveLog.info("Resolve {} targets.", targets.size)

    for (target in targets) {
        val path = target.normalizedPath

        val file = server.root.resolve(path)

        resolveLog.info("Try to resolve [{}]'{}'.", target::class.simpleName, file)

        // Not a file or directory, return empty
        if (!file.exists()) continue

        // Hit a file, return
        if (!file.isDirectory()) return target

        // If server's index resolve is disabled, just return empty
        if(server.noIndexResolve) continue

        // Enter index resolve recursively
        resolveLog.info("'{}' is a directory, try to resolve recursively.", path)
        return resolve(resolveIndexForDirectory(path))
    }

    return null
}

fun Application.installServerRouting() {
    val server = serverContext

    intercept(ApplicationCallPipeline.Call) {
        val requestPath = call.request.path()

        if (server.isScriptEnabled && requestPath.startsWith("/WEB-INF")) {
            return@intercept call.respond(HttpStatusCode.NotFound)
        }

        resolveLog.info("Request URI: '{}'.", call.request.uri)

        val ctx = server.newResolveContext(requestPath, call)

        val targets = ctx.resolveSearchPaths()

        // Resolve on each target
        val callTarget = ctx.resolve(targets)

        call.attributes.put(ResolvedTargetAttributeKey, Optional.ofNullable(callTarget))

        if (callTarget != null) {
            // Call the handler to create and returns a response
            callTarget.handleApplicationCall(call, ctx)
        } else {
            resolveLog.info("Path '{}' not found.", ctx.urlPath)
            call.respondText(status = HttpStatusCode.NotFound) { "Page Not found" }
        }
    }
}

private val ResolvedTargetAttributeKey = AttributeKey<Optional<ResolvedTarget>>("ResolvedTarget")

val ApplicationCall.resolvedTarget: Optional<ResolvedTarget>
    get() = attributes[ResolvedTargetAttributeKey]