package com.shinonometn.template.live.server

import java.nio.file.Path
import kotlin.system.exitProcess

class ServerProfile {
    var port : Int = System.getenv().getOrDefault("PORT", "8080").toInt()
    var engine : String = System.getenv().getOrDefault("ENGINE_NAME", "FREEMARKER")
    var root : Path = Path.of(System.getenv().getOrDefault("PWD", "./")).toAbsolutePath()
    var extensionNameOverwrite = emptyList<String>()

    var isScriptEnabled = false

    companion object {
        fun fromArgs(args: Array<String>) = try {
            parseArgs(args)
        } catch (e : Exception) {
            System.err.println("ERROR: Failed to parse args. ${e.message}")
            System.err.println(helpMessage)
            exitProcess(1)
        }

        fun printHelp() = System.err.println(helpMessage)
    }
}

private val helpMessage = """
Template Engines Live Server

Call with no arguments will start a server with current directory and
listen on port 8080 with FreeMarker template engine

General Options:
    --port       Listen port, default is 8080 (Can be set by PORT)
    
    --engine     Set server engine (Can be set by ENGINE_NAME)
                 Current supported engines:
                     freemarker, velocity, thymeleaf
                    
    --root       Root directory, default is PWD or ./
    
    --ext-list   Additional extension name redirect to templates,
                 seperated with ',', e.g. html,htm

Scripting:
    --enable-groovy   Enable groovy script engine.
                      When groovy is enabled, /WEB-INF will be reserved
                      as private resource directory. Request to /WEB-INF
                      will returns 404.
                
""".trimIndent()

private fun parseArgs(args: Array<String>): ServerProfile {
    val profile = ServerProfile()
    if(args.isEmpty()) return profile
    var index = 0
    var p: String
    while(index < args.size) {
        p = args[index++]
        when(p) {
            "--port" -> profile.port = Integer.parseInt(args[index++])
            "--engine" -> profile.engine = args[index++]
            "--root" -> profile.root = Path.of(args[index++]).toAbsolutePath()
            "--ext-list" -> profile.extensionNameOverwrite = args[index++].split(",")
            "--enable-groovy" -> profile.isScriptEnabled = true
            "--help" -> {
                // Exit if --help is presented
                System.err.println(helpMessage)
                exitProcess(0)
            }
            else -> throw IllegalArgumentException("Unknown argument: $p")
        }
    }
    return profile
}