package com.shinonometn.template.live.server.scripting;

import groovy.lang.Script;

public interface ScriptServiceDelegate {
    /**
     * Name of this script.
     * <p>
     * It's related to the root directory.
     * If the script is in private directory, the name starts with '-'.
     * If the script is in public directory, the name starts with '+'.
     */
    String getScriptName();

    Script require(String scriptName);
}
