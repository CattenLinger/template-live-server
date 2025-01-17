package com.shinonometn.template.live.server.scripting;

import groovy.lang.Script;

import java.io.PrintStream;

public abstract class ServerScriptBase extends Script implements EndpointScript {

    @Override
    public PrintStream getOut() {
        return System.err;
    }

    @Override
    public ScriptResponseDelegate getResponse() {
        return (ScriptResponseDelegate) getBinding().getProperty("response");
    }

    protected Object propertyMissing(String name) {
        if ("writer".equals(name)) return getResponse();
        return null;
    }
}
