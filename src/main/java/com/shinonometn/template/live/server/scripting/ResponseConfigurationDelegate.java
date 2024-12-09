package com.shinonometn.template.live.server.scripting;

import java.io.PrintWriter;

public interface ResponseConfigurationDelegate {
    void contentType(String str);

    void status(Integer statusCode);

    PrintWriter getWriter();
}
