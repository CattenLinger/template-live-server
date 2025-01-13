package com.shinonometn.template.live.server.scripting;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public interface ScriptRequestDelegate {
    String getMethod();
    String getPath();
    String getUrl();
    String getUri();
    String getQueryString();
    String getContentType();

    Map<String, List<String>> getParameters();
    Map<String, List<String>> getHeaders();
    Map<String, String> getCookies();

    @Nullable
    String parameter(@NotNull String name);
    @Nullable
    String header(@NotNull String name);
    @Nullable
    String cookie(@NotNull String name);
}
