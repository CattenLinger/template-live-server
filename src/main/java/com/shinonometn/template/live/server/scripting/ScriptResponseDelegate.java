package com.shinonometn.template.live.server.scripting;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ScriptResponseDelegate {
    void contentType(String str);

    void status(Integer statusCode);

    void leftShift(@NotNull String s);

    Object leftShift(@Nullable Object s);

    void call(@NotNull String s);

    Object call(@Nullable Object o);
}
