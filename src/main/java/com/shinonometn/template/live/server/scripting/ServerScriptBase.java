package com.shinonometn.template.live.server.scripting;

import groovy.lang.Closure;
import groovy.lang.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;

public abstract class ServerScriptBase extends Script {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerScriptBase.class);

    protected final PrintStream out = System.err;
    public final Logger log = LOGGER;

    protected ScriptResponseDelegate getResponse() {
        return (ScriptResponseDelegate) getBinding().getProperty("__Response__");
    }

    protected void response(Closure<Void> closure) {
        closure.setDelegate(getResponse());
        closure.call();
    }

    protected void contentType(String contentType) {
        getResponse().contentType(contentType);
    }

    protected void statusCode(int statusCode) {
        getResponse().status(statusCode);
    }

    protected Object propertyMissing(String name) {
        if ("writer".equals(name)) return getResponse();
        return null;
    }
}
