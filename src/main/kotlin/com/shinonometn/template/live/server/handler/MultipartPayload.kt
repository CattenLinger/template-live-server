package com.shinonometn.template.live.server.handler

import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope

class MultipartPayload internal constructor() : RequestInputDelegate.Payload {
    override val type : String = "multipart"
}