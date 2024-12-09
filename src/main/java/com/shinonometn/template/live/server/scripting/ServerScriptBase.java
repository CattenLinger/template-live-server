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

    protected ResponseConfigurationDelegate getResponse() {
        return (ResponseConfigurationDelegate) getBinding().getProperty("response");
    }

    public void response(Closure<Void> closure) {
        closure.setDelegate(getResponse());
        closure.call();
    }

    protected Object propertyMissing(String name) {
        if ("writer".equals(name)) return getResponse().getWriter();
        return null;
    }
}
