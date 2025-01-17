package com.shinonometn.template.live.server.scripting;

import groovy.lang.Closure;

import java.io.PrintStream;

public interface EndpointScript {

    PrintStream getOut();

    ScriptResponseDelegate getResponse();

    default void response(Closure<Void> closure) {
        closure.setDelegate(getResponse());
        closure.call();
    }

    default void contentType(String contentType) {
        getResponse().contentType(contentType);
    }

    default void statusCode(int statusCode) {
        getResponse().status(statusCode);
    }
}
