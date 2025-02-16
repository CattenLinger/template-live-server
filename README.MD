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
                    
Scripting:
    --enable-groovy   Enable groovy script engine.
                      When groovy is enabled, /WEB-INF will be reserved
                      as private resource directory. Request to /WEB-INF
                      will returns 404.
```

It starts in current directory and serves freemarker templates by default. 
Just like a normal http server, it also serves static contents.

When a request arrive, it will first match a template, then a directory, then a file.
If no match, returns 404.

When a request hits a file on disk, only GET method is allowed.

Template variables
---

The server will put a `request` variable in the template context.
It contains:
- path, the request path
- parameters, all request parameters
- httpMethod, current http request method name

Additional
---

Thymeleaf extension name is '.html', so it will always serve pure html page as thymeleaf template.

Because it is a server for template development, all template caches are disabled for instant refreshing.

Groovy Scripting
---

Script's print output is mapped to `System.err` (stderr).

Script need to interact with the `response` object to send HTTP response.

```groovy
response.statusCode = 200           // Set status code
response.contentType = "text/plain" // Set content type

// Alternative
statusCode  = 200
contentType = "text/plain"
```
There is a special variable `writer`, it's an alias to `response`.
`writer` is the output of current HTTP response, it manages the OutputStream

```groovy
writer << "Hello, world"                // append string to output
writer << new FileOutputStream("file")  // append binary to output
```

The request can be access via `request` property. 

Request body can be access via `body` property. It's a lazy property.

If there is a `server.config.groovy` under `/WEB-INF`, server will evaluate it
before start. Properties set in this script can be access via a global property `server` in handler scripts.

Launch script example
---

```shell
#!/usr/bin/env bash

SERVER_HOME=$(dirname $(realpath $0))

JAR="server.jar"

JAVA_HOME="$(dirname $(realpath $(which java)))/.."
JAVA_HOME="$(realpath $JAVA_HOME)"

JAVA_OPTS="-Xmx128m"

(
printf "Java Home   : $JAVA_HOME\n"
printf "Java Opts   : $JAVA_OPTS\n"
printf "Jar File    : $JAR\n"
printf "Server Home : $SERVER_HOME\n"
printf "\n"
) 1>&2

"$JAVA_HOME"/bin/java $JAVA_OPTS -jar "$SERVER_HOME"/"$JAR" "$@"
```