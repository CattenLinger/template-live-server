package com.shinonometn.template.live.server.scripting

import groovy.lang.Binding
import groovy.util.GroovyScriptEngine
import kotlin.test.Test

class ScriptImportTest {

    @Test
    fun testEngineImporting() {

        val engine = GroovyScriptEngine(arrayOf("./templates/groovy-deps/WEB-INF", "./templates/groovy-deps"))

        for (i in 0 until 10) {
            val script = engine.createScript("test.groovy", Binding())
            script.run()
            Thread.sleep(1000)
        }
    }

}
