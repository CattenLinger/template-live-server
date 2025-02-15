package com.shinonometn.template.live.server.handler

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.*

internal class EndpointRequestDelegateImpl(
    private val call: ApplicationCall, private val bodyProvider: () -> Any?
) : EndpointRequestDelegate {

    override fun getMethod(): String = call.request.httpMethod.value

    override fun getPath(): String = call.request.path()

    override fun getUrl(): String = path

    override fun getUri(): String = call.request.uri

    override fun getQueryString(): String = call.request.queryString()

    override fun getContentType(): String = call.request.contentType().toString()

    private val parameterMap by lazy {
        call.parameters.toMap().mapValues { it.value.toMutableList() }.toMutableMap()
    }

    override fun getParameters(): MutableMap<String, MutableList<String>> = parameterMap

    private val headerMap by lazy {
        call.request.headers.toMap().mapValues { it.value.toMutableList() }.toMutableMap()
    }

    override fun getHeaders(): MutableMap<String, MutableList<String>> = headerMap

    private val cookieMap by lazy { call.request.cookies.rawCookies.toMutableMap() }
    override fun getCookies(): MutableMap<String, String> = cookieMap

    override fun parameter(name: String): String? = call.parameters[name]

    override fun header(name: String): String? = call.request.header(name)

    override fun cookie(name: String): String? = call.request.cookies[name]

    override fun getBody(): Any? = bodyProvider()
}