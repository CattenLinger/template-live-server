Live Template Dev Server
===

This is a simple server to quickly launch template preview for Freemarker, Velocity and Thymeleaf.

Usage:
```
java -jar server.jar

Arguments:
    --port      Listen port, default is 8080 (Can be set by PORT)
    --engine    Set server engine (Can be set by ENGINE_NAME)
                    Current supported engines:
                    freemarker, velocity, thymeleaf
    --root      Root directory (default is PWD)
    --ext-list  Additional extension name redirect to templates.
                    Seperated with ',', e.g. html,htm
```

It starts in current directory and serves freemarker templates by default. 
Just like a normal http server, it also serves static contents.

When a request arrive, it will first match a template, then a directory, then a file.
If no match, returns 404.

When a request hits a file on disk, only GET method is allowed.

## Template variables

The server will put a `_request` variable in the template context.
It contains:
- path, the request path
- parameters, all request parameters
- httpMethod, current http request method name

## Additional

Thymeleaf extension name is '.html', so it will always serve pure html page as thymeleaf template.

Because it is a server for template development, all template caches are disabled for instant refreshing.

